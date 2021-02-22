package com.mkweb.core;

import com.mkweb.config.MkConfigReader;
import com.mkweb.config.MkFTPConfigs;
import com.mkweb.config.MkPageConfigs;
import com.mkweb.data.MkFtpData;
import com.mkweb.data.MkPageJsonData;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.ConnectionChecker;
import com.mkweb.utils.MyCrypto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

@WebServlet(name = "MkFTPServlet", loadOnStartup = 1)
@MultipartConfig
public class MkFileReceiver extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private MkLogger mklogger = MkLogger.Me();
	private String TAG = "[MkFileReceiver]";
	private MkPageJsonData pjData = null;
	private ArrayList<MkPageJsonData> pi = null;
	private boolean isPiSet = false;
	MkPageJsonData pageStaticData = null;
	private static final String HASH_PREFIX = "__TRIP_!!_DIARY__";
	private String requestParameterName = null;
	private ArrayList<String> requestValues = null;
	private ConnectionChecker cpi = null;
	
	private List<Part> fileParts = null;

	public MkFileReceiver() {
		this.cpi = new ConnectionChecker();
	}

	private ArrayList<MkPageJsonData> getPageControl(String url) {
		/*
		String[] requestUriList = url.split("/");
		String mkPage = "/" + requestUriList[requestUriList.length - 1];
		mklogger.debug(this.TAG, "mkPage : " + mkPage);
		if (mkPage.equals(MkConfigReader.Me().get("mkweb.web.hostname")))
			mkPage = ""; 
		
		mklogger.debug(this.TAG, "receive mkpage : " + mkPage);
		*/
		String mkPage = null;
		String hostcheck = url.split("://")[1];
		String host = MkConfigReader.Me().get("mkweb.web.hostname") + "/";
		if(hostcheck.contentEquals(host)) {
			mkPage = "";
		}else {
			mkPage = "/" + hostcheck.split(host)[1];
		}
		mklogger.debug(TAG, "mkpage :" + mkPage);
		return MkPageConfigs.Me().getControl(mkPage);
	}

	private boolean checkMethod(HttpServletRequest request, String rqMethod, String rqPageURL) {
		String hostCheck = rqPageURL.split("://")[1];
		String host = MkConfigReader.Me().get("mkweb.web.hostname");
		if (host == null) {
			this.mklogger.error(this.TAG, " Hostname is not set. You must set hostname on configs/MkWeb.conf");
			return false;
		} 
		host = String.valueOf(host) + "/";
		String requestURI = rqPageURL.split(MkConfigReader.Me().get("mkweb.web.hostname"))[1];
		String mkPage = !hostCheck.contentEquals(host) ? requestURI : "";
		if (!this.cpi.isValidPageConnection(mkPage)) {
			this.mklogger.error(this.TAG, " checkMethod: Invalid Page Connection.");
			return false;
		} 
		if (this.pi == null || !this.isPiSet) {
			this.mklogger.error(this.TAG, " PageInfo is not set!");
			return false;
		}
		
		ArrayList<MkPageJsonData> pal = MkPageConfigs.Me().getControl(mkPage);
		for (MkPageJsonData pj : pal) {
			if (pj.getParameter().equals(this.requestParameterName)) {
				this.pjData = pj;
				break;
			} 
		} 

		try {
			if (!this.pjData.getMethod().toLowerCase().contentEquals(rqMethod))
				return false; 
		} catch (NullPointerException e) {
			mklogger.error(TAG, "There is no service for request parameter. You can ignore 'Request method is not authorized.' error.");
			mklogger.debug(TAG, "Page Json Data is Null");
			return false;
		}

		//	this.requestValues = this.cpi.getRequestParameterValues(request, this.pjData.getParameter(), this.pageStaticData);
		return (this.pjData != null);
	}

	private void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String previousURL = request.getHeader("referer");
		if(!cpi.comparePageValueWithRequestValue(pjData.getPageValue(), requestValues, pageStaticData, false, false)) {
			mklogger.error(TAG, " Request Value is not authorized. Please check page config.");
			response.sendError(400);
			return;
		}

		String control = this.pjData.getControlName();
		String service = this.pjData.getServiceName();
		
		this.mklogger.debug(this.TAG, "controller: " + control + ", service : " + service);
		ArrayList<MkFtpData> ftpControl = MkFTPConfigs.Me().getControlByServiceName(service);

		MkFtpData ftpService = null;
		for (MkFtpData mfd : ftpControl) {
			if (mfd.getServiceName().contentEquals(service)) {
				ftpService = mfd;
				break;
			}
		}
		
		/* Ȯ���� Ȯ�� */
		String[] allowFormats = ftpService.getData();
		int size = fileParts.size();
		ArrayList<InputStream> fileContents = new ArrayList<>();
		String[] fileNames = new String[size];
		int currentIndex = 0;
		for (Part filePart : fileParts) {
			String partName = filePart.getName();
			if(partName != null && !partName.contentEquals("")) {
				if(partName.contains(".")) {
					if(partName.split("\\.")[1].contains(ftpService.getDirPrefix())) {
						continue;
					}
				}
			}
			
			fileNames[currentIndex] = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MSIE fix.
			fileContents.add(filePart.getInputStream());
			
			if(fileNames[currentIndex].contains(".")) {
				String[] periods = fileNames[currentIndex].split("\\.");
				String extension = periods[periods.length-1];
				
				boolean passed = false;
				for(String format : allowFormats) {
					if(extension.contentEquals(format)) {
						passed = true;
					}
				}
				if(!passed) {
					mklogger.error(TAG, "The received file have format which is not supported.");
					return;
				}
			}
			currentIndex++;
		}
		
		String filePath = ftpService.getPath();
		
		currentIndex = 0;

		String ftpDirPrefix = ftpService.getDirPrefix();
		String[] dirs = null;
		boolean ftpDirHash = ftpService.getHashDirPrefix();
		mklogger.debug(TAG, "ftpDirPrefix : " + ftpDirPrefix + ", ftpDirHash : "+ ftpDirHash);
		if(ftpDirPrefix != null) {
			ftpDirPrefix = request.getParameter((pjData.getParameter() + "." + ftpDirPrefix));
			mklogger.debug(TAG, "2 ftpDirPrefix : " + ftpDirPrefix);
			if(ftpDirPrefix == null) {
				mklogger.error(TAG, "User didn't send dir prefix.");
				response.sendError(400, "Need to send path.");
				return;
			}
			
			if(ftpDirPrefix.contains("^")) {
				dirs = ftpDirPrefix.split("\\^");
				String tempDir = "";
				for(String dir : dirs) {
					if(ftpDirHash)
						tempDir += "/" + new MyCrypto().MD5(dir + HASH_PREFIX);
					else
						tempDir += "/" + dir;
				}
				
				ftpDirPrefix = tempDir;
			}else {
				if(ftpDirHash) 
					ftpDirPrefix = "/" + new MyCrypto().MD5(ftpDirPrefix + HASH_PREFIX);
				else
					ftpDirPrefix = "/" + ftpDirPrefix;
			}
		}else {
			ftpDirPrefix = "";
		}
		
		filePath = filePath + ftpDirPrefix;
		filePath = MkFTPConfigs.Me().getPrefix() + filePath; 
		mklogger.debug(TAG, "file Path : " + filePath + "/ fileNames /" + fileNames[currentIndex]);
		
		/*
		 * filePath Check
		 */
		if(!ftpDirPrefix.contentEquals("") || ftpDirPrefix != null) {
			File folder = new File(filePath);
			boolean isDirExists = folder.exists();
			if(!isDirExists)
			{
				mklogger.info(TAG, "The directory is not exists. Creating new one... : ");
				try {		
					isDirExists = folder.mkdirs();
					folder.setReadable(true, false);
					folder.setExecutable(true, false);
					if(!isDirExists) {
						mklogger.error(TAG, "Failed to create path. [" + filePath +"]");
						response.sendError(500, "Server have some problems! Please contact Admin.");
						return;
					}
				} catch (Exception e) {
					mklogger.error(TAG, "Failed to create path. [" + filePath +"] " + e.getMessage());
					response.sendError(500, "Server have some problems! Please contact Admin.");
					return;
				}
			}
		}
		
		for(InputStream fileContent : fileContents) {
			File currentFile = new File(filePath + "/" + fileNames[currentIndex++]);
			try {
				currentFile.createNewFile();
			} catch (IOException e) {
				mklogger.error(TAG, "Failed to upload file. Maybe there is no target directory." + e.getMessage());
				continue;
			}
			
			currentFile.setReadable(true, false);
			currentFile.setExecutable(true, false);
			try(FileOutputStream outputStream = new FileOutputStream(currentFile)){
				int read;
				byte[] bytes = new byte[1024];
				
				while((read = fileContent.read(bytes)) != -1) {
					outputStream.write(bytes, 0, read);
				}
			} catch (Exception e) {
				mklogger.temp(TAG + "There was something wrong to create file : " + currentFile.getAbsolutePath() + "/ file name : " + currentFile.getName(), false);
				mklogger.temp(TAG + e.getMessage(), false);
				mklogger.flush("error");
			}
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!MkConfigReader.Me().get("mkweb.ftp.use").contentEquals("yes")) {
			mklogger.temp(TAG + "MkReceiveFormData is not allowed. But user tried to use it. However, this log only show when web.xml have this servlet information. Please modify web.xml or change your MkWeb setting.", false);
			mklogger.temp("Also if you are not going to use MkReceiveFormData, and not going to change web.xml, the /data/receive uri is being dead.", false);
			mklogger.flush("error");
			mklogger.debug(TAG, "mkweb.web.receive.use is not yes.");
			return;
		} 
		String refURL = request.getHeader("Referer");
		pi = getPageControl(refURL);
		isPiSet = (this.pi != null);
		pageStaticData = null;
		if (this.isPiSet) {
			int i;
			for (i = 0; i < this.pi.size(); i++) {
				if (((MkPageJsonData)this.pi.get(i)).getPageStatic()) {
					pageStaticData = this.pi.get(i);
					break;
				} 
			} 
		} 

		if(!prepareToReceiveFiles(request)) {
			mklogger.error(TAG, "Failed to prepare Receive files");
			return;
		}

		if (!checkMethod(request, "get", refURL)) {
			this.mklogger.error(this.TAG, " Request method is not authorized. [Tried: GET]");
			response.sendError(401);
			return;
		} 
		doTask(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (!MkConfigReader.Me().get("mkweb.ftp.use").contentEquals("yes")) {
			mklogger.temp(TAG + "MkReceiveFormData is not allowed. But user tried to use it. However, this log only show when web.xml have this servlet information. Please modify web.xml or change your MkWeb setting.", false);
			mklogger.temp("Also if you are not going to use MkReceiveFormData, and not going to change web.xml, the /data/receive uri is being dead.", false);
			mklogger.flush("error");
			mklogger.debug(TAG, "mkweb.web.receive.use is not yes. Please check MkWeb.conf");
			return;
		}

		String refURL = request.getHeader("Referer");
		
		pi = getPageControl(refURL);
		isPiSet = (pi != null);
		pageStaticData = null;
		if (isPiSet) {
			int i;
			for (i = 0; i < this.pi.size(); i++) {
				if (((MkPageJsonData)pi.get(i)).getPageStatic()) {
					pageStaticData = pi.get(i);
					break;
				} 
			} 
		} 

		if(!prepareToReceiveFiles(request)) {
			mklogger.error(TAG, "Failed to prepare Receive files");
			return;
		}

		if (!checkMethod(request, "post", refURL)) {
			this.mklogger.error(TAG, " Request method is not authorized. [Tried: POST]");
			response.sendError(401);
			return;
		} 
		doTask(request, response);
	}

	private String getSubmittedFileName(Part part) {
		for (String cd : part.getHeader("content-disposition").split(";")) {
			if (cd.trim().startsWith("filename")) {
				String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
				return fileName.substring(fileName.lastIndexOf('/') + 1).substring(fileName.lastIndexOf('\\') + 1); // MSIE fix.
			}
		}
		return null;
	}


	private String getSubmittedParameterName(String datas) {
		mklogger.debug(TAG, "datas : " + datas);
		return datas.substring(0, datas.indexOf('.'));
	}

	private String[] getSubmittedParameters(String formData) {
		String[] splitDatas = formData.split("Content-Disposition");
		String[] result = new String[splitDatas.length-1];
		if(splitDatas.length < 1) {
			return null;
		}
		
		for(int i = 1; i < splitDatas.length; i++) {
			String fd = splitDatas[i].split("Content-Type")[0];
			result[i-1] = getSubmittedParameterName(fd);
			mklogger.debug(TAG, "result i : " + result[i-1]);
		}
		return result;
	}
	
	private static String getSubmittedParameters(Part part) {
		for(String pd : part.getHeader("content-disposition").split(";")) {
			if(pd.trim().startsWith("name")) {
				String parameterName = pd.substring(pd.indexOf('=') + 1).trim().replace("\"", "");
				return parameterName.substring(parameterName.lastIndexOf('/') + 1).substring(parameterName.lastIndexOf('\\') + 1); // MSIE fix.
			}
		}
		return null;
	}

	private boolean prepareToReceiveFiles(HttpServletRequest request) throws IOException, ServletException {
		fileParts = (List<Part>) request.getParts();
		
		HashMap<String, ArrayList<String>> requestPartsParameters = new HashMap<>();
		
		/* ��ȿ�� �˻� */
		for(Part part : fileParts) {
			String partParameter = getSubmittedParameters(part);
			String partParameterName = getSubmittedParameterName(partParameter);
			
			ArrayList<String> temp = null;
			
			temp = (requestPartsParameters.get(partParameterName) == null) ? new ArrayList<>() : requestPartsParameters.get(partParameterName);
			
			temp.add(partParameter.split("\\.")[1]);
			requestPartsParameters.put(partParameterName, temp);
		}
		
		if(requestPartsParameters.size() != 1) {
			mklogger.error(TAG, "�� �� ���񽺰� ���ÿ� ��û��~");
			return false;
		}
		/* ��ȿ�� �˻� */
	
		requestParameterName = requestPartsParameters.keySet().iterator().next();
		requestValues = requestPartsParameters.get(requestParameterName);
		
		return true;
	}
}
