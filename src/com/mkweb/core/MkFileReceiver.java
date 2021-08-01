package com.mkweb.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.json.simple.JSONObject;

import com.mkweb.config.MkConfigReader;
import com.mkweb.config.MkFTPConfigs;
import com.mkweb.config.MkPageConfigs;
import com.mkweb.data.MkFtpData;
import com.mkweb.utils.MkJsonData;
import com.mkweb.data.MkPageJsonData;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.ConnectionChecker;
import com.mkweb.utils.MkCrypto;

@WebServlet(name = "MkFTPServlet", loadOnStartup = 1)
@MultipartConfig
public class MkFileReceiver extends HttpServlet {
	/*
	Problem: 이미지를 동적인 N개 보내기를 원하지만 현재 ftp 콘픽 설정에서는 정적의 개수를 설정해야 한다.
	최대개수를 설정해 놓고, 없으면 취소하는건 어떤가?
	예를 들어서, 그럼 내가 이미지와 한글파일을 따로 저장하고 싶으면?? 이건 아닌것같은데..
	OK. 이제 정리 됨!
	 */
	private static final long serialVersionUID = 1L;
	
	private static final String TAG = "[MkFileReceiver]";
	private static final MkLogger mklogger = new MkLogger(TAG);
	
	private MkPageJsonData pjData = null;
	private ArrayList<MkPageJsonData> pi = null;
	private boolean isPiSet = false;
	MkPageJsonData pageStaticData = null;
	private static final String HASH_PREFIX = MkConfigReader.Me().get("mkweb.ftp.hash.prefix");
	private String requestParameterName = null;
	private ArrayList<String> requestValues = null;
	private HashMap<String, String> deleteParameters = null;
	
	private static final String[] FTP_MODE = {"ftp-receiver", "ftp-remover"};
	
	private List<Part> fileParts = null;

	private ArrayList<MkPageJsonData> getPageControl(String url) {
		String mkPage = null;
		String hostcheck = url.split("://")[1];
		String host = MkConfigReader.Me().get("mkweb.web.hostname") + "/";
		if(hostcheck.contentEquals(host)) {
			mkPage = "";
		}else {
			mkPage = "/" + hostcheck.split(host)[1];
		}
		mklogger.debug("mkpage :" + mkPage);
		return MkPageConfigs.Me().getControl(mkPage);
	}

	private boolean checkMethod(HttpServletRequest request, String rqMethod, String rqPageURL) {
		String hostCheck = rqPageURL.split("://")[1];
		String host = MkConfigReader.Me().get("mkweb.web.hostname");
		
		mklogger.debug("method : " + rqMethod);
		if (host == null) {
			mklogger.error(" Hostname is not set. You must set hostname on configs/MkWeb.conf");
			return false;
		} 
		host = String.valueOf(host) + "/";
		String requestURI = rqPageURL.split(MkConfigReader.Me().get("mkweb.web.hostname"))[1];
		String mkPage = !hostCheck.contentEquals(host) ? requestURI : "";
		if (!ConnectionChecker.isValidPageConnection(mkPage)) {
			mklogger.error(" checkMethod: Invalid Page Connection.");
			return false;
		} 
		if (this.pi == null || !this.isPiSet) {
			mklogger.error(" PageInfo is not set!");
			return false;
		}
		
		ArrayList<MkPageJsonData> pal = MkPageConfigs.Me().getControl(mkPage);
		for (MkPageJsonData pj : pal) {
			if (pj.getParameter().equals(this.requestParameterName)) {
				mklogger.debug("requestParameterName : " + requestParameterName);
				this.pjData = pj;
				break;
			} 
		}

		try {
			if (!this.pjData.getMethod().toLowerCase().contentEquals(rqMethod)) {
				mklogger.error("data service : " + pjData.getServiceName());
				mklogger.error("no method matched | page : " + pjData.getMethod() + ", requested : " + rqMethod);
				return false;
			}
				 
		} catch (NullPointerException e) {
			mklogger.error("There is no service for request parameter. You can ignore 'Request method is not authorized.' error.");
			mklogger.debug("Page Json Data is Null");
			return false;
		}

		//	this.requestValues = this.cpi.getRequestParameterValues(request, this.pjData.getParameter(), this.pageStaticData);
		return (this.pjData != null);
	}

	private void doTask(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");

		String control = this.pjData.getControlName();
		String service = this.pjData.getServiceName();

		boolean isDone = false;
		mklogger.debug("controller: " + control + ", service : " + service);
		ArrayList<MkFtpData> ftpControl = MkFTPConfigs.Me().getControlByServiceName(service);

		MkFtpData ftpService = null;
		for (MkFtpData mfd : ftpControl) {
			if (mfd.getServiceName().contentEquals(service)) {
				ftpService = mfd;
				break;
			}
		}
		/*
		여기서 파일 갯수 수정
		*/
		if(!ConnectionChecker.compareFtpPageValueWithRequestValue(pjData.getPageValue(), requestValues, pageStaticData, ftpService, false)) {
			mklogger.error(" Request Value is not authorized. Please check page config.");
			response.sendError(400);
			return;
		}

		String result = "";
		StringBuilder responseMsg = new StringBuilder();
		StringBuilder uploaded = new StringBuilder();
		int responseCode = -1;
		
		if(ftpService == null) {
			responseMsg = new StringBuilder("No further information.");
			responseCode = 400;
			mklogger.error("There is no FTP service named : " + service);
		}
		while(!isDone) {
			if(!Arrays.asList(FTP_MODE).contains(ftpService.getServiceType())) {
				isDone = true;
				responseMsg = new StringBuilder("This cannot receive a file.");
				responseCode = 400;
				mklogger.error("This service is not kind of file receiver. Please check FTP controller, or request parameters");
				break;
			}
			
			String[] allowFormats = ftpService.getData();
			
			if(isDone)
				break;
			/* duplicate code */
			String filePath = ftpService.getPath();
			String ftpDirPrefix = ftpService.getDirPrefix();
			String[] dirs = null;
			boolean ftpDirHash = ftpService.getHashDirPrefix();
			mklogger.debug("ftpDirPrefix : " + ftpDirPrefix + ", ftpDirHash : "+ ftpDirHash);
			if(ftpDirPrefix != null) {
				ftpDirPrefix = request.getParameter((pjData.getParameter() + "." + ftpDirPrefix));
				mklogger.debug("2 ftpDirPrefix : " + ftpDirPrefix);
				if(ftpDirPrefix == null) {
					mklogger.error("User didn't send dir prefix.");
					
					responseCode = 400;
					responseMsg = new StringBuilder("Need to send path.");
					
					isDone = true;
					break;
				}
				
				if(ftpDirPrefix.contains("^")) {
					dirs = ftpDirPrefix.split("\\^");
					StringBuilder tempDir = new StringBuilder();
					for(String dir : dirs) {
						if(ftpDirHash)
							tempDir.append("/").append(MkCrypto.MD5(dir + HASH_PREFIX));
						else
							tempDir.append("/").append(dir);
					}
					
					ftpDirPrefix = tempDir.toString();
				}else {
					if(ftpDirHash) 
						ftpDirPrefix = "/" + MkCrypto.MD5(ftpDirPrefix + HASH_PREFIX);
					else
						ftpDirPrefix = "/" + ftpDirPrefix;
				}
			}else {
				ftpDirPrefix = "";
			}

			if(ftpDirPrefix.isEmpty()){
				filePath = filePath + "/" + System.currentTimeMillis();
				mklogger.debug("filePath 1 : " + filePath);
			} else {
				filePath = filePath + ftpDirPrefix;
				mklogger.debug("filePath 2 : " + filePath);
			}

			filePath = MkFTPConfigs.Me().getPrefix() + filePath;
			
			if(ftpService.getServiceType().contentEquals("ftp-receiver")) {
				/* Receive Mode */
				if(responseCode == -1)
					responseCode = 201;
				int size = fileParts.size();
				ArrayList<InputStream> fileContents = new ArrayList<>();
				String[] fileNames = new String[size];
				int currentIndex = 0;
				for (Part filePart : fileParts) {
					String partName = filePart.getName();
					if(partName != null && !partName.contentEquals("")) {
						mklogger.debug("partName : " + partName + " | ftpDirPrefix: " + ftpDirPrefix + "!" + " is empty : " + ftpDirPrefix.isEmpty());
						if(partName.contains(".") && !ftpDirPrefix.isEmpty()) {
							if(partName.split("\\.")[1].contains(ftpDirPrefix)) {
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
							mklogger.error("The received file have format which is not supported.");
							return;
						}
					}
					currentIndex++;
				}

				String folderName = null;
				if(!ftpDirPrefix.contentEquals("") || ftpDirPrefix != null) {
					File folder = new File(filePath);
					folderName = folder.getPath();
					boolean isDirExists = folder.exists();
					if(!isDirExists)
					{
						mklogger.info("The directory is not exists. Creating new one... " + folder.getPath());
						try {
							isDirExists = folder.mkdirs();
							folder.setReadable(true, false);
							folder.setExecutable(true, false);
							if(!isDirExists) {
								responseCode = 500;
								responseMsg = new StringBuilder("Server have some problems! Please contact Admin.");
								mklogger.error( "Failed to create path. [" + filePath +"]");
								isDone = true;
								break;
							}
						} catch (Exception e) {
							mklogger.error("Failed to create path. [" + filePath +"] " + e.getMessage());
							responseCode = 500;
							responseMsg = new StringBuilder("Server have some problems! Please contact Admin.");
							isDone = true;
							break;
						}
					}
				}
				currentIndex = 0;
				for(InputStream fileContent : fileContents) {
					File currentFile = new File(filePath + "/" + fileNames[currentIndex++]);
					try {
						currentFile.createNewFile();
					} catch (IOException e) {
						mklogger.error("Failed to upload file. Maybe there is no target directory." + e.getMessage());
						responseCode = 500;
						responseMsg = new StringBuilder("Server have some problems! Please contact Admin.");
						
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

						String responsePrefix = ftpService.getPath() + "/" + currentFile.getPath().split(ftpService.getPath())[1];
						responsePrefix = responsePrefix.replaceAll("\\/\\/", "/");
						uploaded.append(responsePrefix);
						responseMsg.append("[Success to upload : ").append(responsePrefix).append("]");
						if(currentIndex < fileNames.length){
							uploaded.append(",");
							responseMsg.append(",");
						}
					} catch (Exception e) {
						if(!currentFile.getAbsolutePath().contentEquals(folderName)){
							mklogger.temp("There was something wrong to create file : " + currentFile.getAbsolutePath() + "/ file name : " + currentFile.getName(), false);
							mklogger.temp(e.getMessage(), false);
							mklogger.flush("error");

							responseCode = 401;
							responseMsg.append("[Fail to upload : ").append(currentFile.getAbsolutePath()).append("],");
						}
					}
				}
			}
		
			isDone = true;
			break;
		}
		
		response.setStatus(responseCode);
		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = response.getWriter();
		result = "{\"code\":\"201\",\"response\":\"" + responseMsg + "\",\"excuted\":\"" + uploaded + "\"}";
		out.print(result);
		out.flush();
		out.close();
	}
	
	private void doDeleteFile(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");

		String control = this.pjData.getControlName();
		String service = this.pjData.getServiceName();

		boolean isDone = false;
		mklogger.debug("controller: " + control + ", service : " + service);
		ArrayList<MkFtpData> ftpControl = MkFTPConfigs.Me().getControlByServiceName(service);

		MkFtpData ftpService = null;
		for (MkFtpData mfd : ftpControl) {
			if (mfd.getServiceName().contentEquals(service)) {
				ftpService = mfd;
				break;
			}
		}
		
		String result = "";
		String responseMsg = "";
		String uploaded = "";
		int responseCode = -1;
		
		if(ftpService == null) {
			responseMsg = "No further information.";
			responseCode = 400;
			mklogger.error("There is no FTP service named : " + service);
		}
		while(!isDone) {
			
			if(!Arrays.asList(FTP_MODE).contains(ftpService.getServiceType())) {
				isDone = true;
				responseMsg = "This cannot receive a file.";
				responseCode = 400;
				mklogger.error("This service is not kind of file receiver. Please check FTP controller, or request parameters");
				break;
			}
			
			String[] allowFormats = ftpService.getData();
			
			if(isDone)
				break;
			
			String filePath = ftpService.getPath();
			String ftpDirPrefix = ftpService.getDirPrefix();
			String[] dirs = null;
			boolean ftpDirHash = ftpService.getHashDirPrefix();
			mklogger.debug("ftpDirPrefix : " + ftpDirPrefix + ", ftpDirHash : "+ ftpDirHash);
			
			if(ftpDirPrefix != null) {
				ftpDirPrefix = deleteParameters.get(pjData.getParameter() + "." + ftpDirPrefix);
				deleteParameters.remove(pjData.getParameter() + "." + ftpDirPrefix);
				mklogger.debug("2 ftpDirPrefix : " + ftpDirPrefix);
				if(ftpDirPrefix == null) {
					mklogger.error("User didn't send dir prefix.");
					
					responseCode = 400;
					responseMsg = "Need to send path.";
					
					isDone = true;
					break;
				}
				
				if(ftpDirPrefix.contains("^")) {
					dirs = ftpDirPrefix.split("\\^");
					StringBuilder tempDir = new StringBuilder();
					for(String dir : dirs) {
						if(ftpDirHash)
							tempDir.append("/").append(MkCrypto.MD5(dir + HASH_PREFIX));
						else
							tempDir.append("/").append(dir);
					}
					
					ftpDirPrefix = tempDir.toString();
				}else {
					if(ftpDirHash) 
						ftpDirPrefix = "/" + MkCrypto.MD5(ftpDirPrefix + HASH_PREFIX);
					else
						ftpDirPrefix = "/" + ftpDirPrefix;
				}
			}else {
				ftpDirPrefix = "";
			}
			
			filePath = filePath + ftpDirPrefix;
			filePath = MkFTPConfigs.Me().getPrefix() + filePath;
			
			if(ftpService.getServiceType().contentEquals("ftp-remover")) {
				/* Removing Mode */
				if(responseCode == -1)
					responseCode = 204;
				
				/*Need to update : Supporting multiple remove request*/

				Set<String> dpSet = deleteParameters.keySet();
				Iterator<String> iter = dpSet.iterator();
				
				String removeTarget = null;
				if(iter.hasNext()) {
					removeTarget = deleteParameters.get(iter.next());
				}else {
					responseCode = 400;
					responseMsg = "No further information.";
					mklogger.error("To remove file, must specify the target file.");
					break;
				}
				
				String newName = null;
				try{
					newName = removeTarget.split("\\^")[0] + "." + removeTarget.split("\\^")[1];
				} catch (NullPointerException e) {
					mklogger.warn("File extension is not valid. return HTTP.204");
					responseCode = 204;
					isDone = true;
					break;
				}
				
				mklogger.debug("So removeTarget is : " + removeTarget + "|newName: " + newName);
				
				File ftpResults = new File(filePath + "/" + newName);
				
				if(!ftpResults.exists()) {
					responseCode = 204;
					mklogger.warn("File is not exists. However, return HTTP.204");
					isDone = true;
					break;
				}
				
				try {
					if(ftpResults.delete()) {
						responseCode = 204;
						uploaded = ftpResults.getName();
						mklogger.info("Success to remove file : " + ftpResults.getPath() + " / " + ftpResults.getName());
						isDone = true;
						break;
					}else {
						responseCode = 500;
						responseMsg = "Some error occured while deleting file. Please contact Admin.";
						mklogger.info("Failed to remove file : " + ftpResults.getPath() + " / " + ftpResults.getName());
					}
				} catch (Exception e) {
					mklogger.temp("Some error occured while deleting file.", false);
					mklogger.temp(e.getMessage(), false);
					mklogger.flush("error");
					responseCode = 500;
					responseMsg = "Some error occured while deleting file. Please contact Admin.";
					isDone = true;
					break;
				}
			}
			
			isDone = true;
			break;
		}
		
		response.setStatus(responseCode);
		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = response.getWriter();
		result = "{\"code\":\"201\",\"response\":\"" + responseMsg + "\",\"excuted\":\"" + uploaded + "\"}";
		out.print(result);
		out.flush();
		out.close();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		if (!MkConfigReader.Me().get("mkweb.ftp.use").contentEquals("yes")) {
//			mklogger.temp("MkReceiveFormData is not allowed. But user tried to use it. However, this log only show when web.xml have this servlet information. Please modify web.xml or change your MkWeb setting.", false);
//			mklogger.temp("Also if you are not going to use MkReceiveFormData, and not going to change web.xml, the /data/receive uri is being dead.", false);
//			mklogger.flush("error");
//			mklogger.debug("mkweb.web.receive.use is not yes.");
//			return;
//		}
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
			mklogger.error("Failed to prepare Receive files");
			response.sendError(400);
			return;
		}
		
		if (!checkMethod(request, "get", refURL)) {
			mklogger.error(" Request method is not authorized. [Tried: GET]");
			response.sendError(405);
			return;
		} 
		doTask(request, response);
	}
	
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
//		if (!MkConfigReader.Me().get("mkweb.ftp.use").contentEquals("yes")) {
//			mklogger.temp("MkReceiveFormData is not allowed. But user tried to use it. However, this log only show when web.xml have this servlet information. Please modify web.xml or change your MkWeb setting.", false);
//			mklogger.temp("Also if you are not going to use MkReceiveFormData, and not going to change web.xml, the /data/receive uri is being dead.", false);
//			mklogger.flush("error");
//			mklogger.debug("mkweb.web.receive.use is not yes.");
//			return;
//		}
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
		deleteParameters = new HashMap<>();
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;
		try(InputStream inputStream = request.getInputStream()){
			if(inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[256];
				int bytesRead = -1;
				while((bytesRead = bufferedReader.read(charBuffer)) > 0) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if(bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch(IOException e) {
					throw e;
				}
			}
		}
		
		mklogger.debug("\n\nSB :" + stringBuilder.toString());
		String rawData = stringBuilder.toString();
		
		MkJsonData jsonData = new MkJsonData(rawData);
		if(!jsonData.setJsonObject()) {
			mklogger.error("Request parameters must be tyoeof JSONObject");
			response.sendError(400);
			return;
		} 

		JSONObject jsonObject = jsonData.getJsonObject();
		
		/* Rewritable..? */
		Set<String> keys = jsonObject.keySet();
		Iterator<String> iter = keys.iterator();
		
		HashMap<String, String> paramKeys = new HashMap<>();
		String currentKey = null;
		while(iter.hasNext()) {
			String key = iter.next();
			if(!key.contains(".")) {
				mklogger.error("Request parameter doesn't have parameter.");
				response.sendError(400);
				break;
			}
			
			String param = key.split("\\.")[0];
			paramKeys.put(param, "true");
			if(currentKey == null) {
				deleteParameters.put(key, jsonObject.get(key).toString());
				currentKey = param;
				continue;
			}
			
			if(param.contentEquals(currentKey)) {
				deleteParameters.put(key, jsonObject.get(key).toString());
				currentKey = param;
			}
		}
		
		if(paramKeys.size() > 1){
			mklogger.error("File Remover only accept 1 service. However, client requested : " + paramKeys.size());
			response.sendError(400);
			return;
		}
		
		if(deleteParameters.size() > 2) {
			mklogger.error("Now only supports remove single file.");
			response.sendError(400);
			return;
		}
		
		
		requestParameterName = currentKey;
		
		mklogger.debug("requestParameterName: " + currentKey + "(currentkey)");
		
		String[] datas = rawData.split("&");
		if (!checkMethod(request, "delete", refURL)) {
			mklogger.error("Request method is not authorized. [Tried: DELETE]");
			response.sendError(405);
			return;
		} 
		doDeleteFile(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		if (!MkConfigReader.Me().get("mkweb.ftp.use").contentEquals("yes")) {
//			mklogger.temp("MkReceiveFormData is not allowed. But user tried to use it. However, this log only show when web.xml have this servlet information. Please modify web.xml or change your MkWeb setting.", false);
//			mklogger.temp("Also if you are not going to use MkReceiveFormData, and not going to change web.xml, the /data/receive uri is being dead.", false);
//			mklogger.flush("error");
//			mklogger.debug("mkweb.web.receive.use is not yes. Please check MkWeb.conf");
//			return;
//		}

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
			mklogger.error("Failed to prepare Receive files");
			return;
		}

		if (!checkMethod(request, "post", refURL)) {
			mklogger.error(" Request method is not authorized. [Tried: POST]");
			response.sendError(405);
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
		mklogger.debug("datas : " + datas);
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
			mklogger.debug("result i : " + result[i-1]);
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
		
		for(Part part : fileParts) {
			String partParameter = getSubmittedParameters(part);
			String partParameterName = getSubmittedParameterName(partParameter);
			
			ArrayList<String> temp = null;
			
			temp = (requestPartsParameters.get(partParameterName) == null) ? new ArrayList<>() : requestPartsParameters.get(partParameterName);
			
			temp.add(partParameter.split("\\.")[1]);
			requestPartsParameters.put(partParameterName, temp);
		}
		
		if(requestPartsParameters.size() != 1) {
			mklogger.error("request Parameter Size .. 1아님.. 뭔소리지..");
			return false;
		}
	
		requestParameterName = requestPartsParameters.keySet().iterator().next();
		requestValues = requestPartsParameters.get(requestParameterName);
		return true;
	}
}