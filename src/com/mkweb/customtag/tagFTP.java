package com.mkweb.customtag;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.mkweb.data.MkFtpData;
import com.mkweb.data.MkPageJsonData;
import com.mkweb.logger.MkLogger;
import com.mkweb.utils.ConnectionChecker;
import com.mkweb.utils.MkCrypto;
import com.mkweb.config.MkConfigReader;
import com.mkweb.config.MkFTPConfigs;
import com.mkweb.config.MkPageConfigs;

public class tagFTP extends SimpleTagSupport {
	private String obj;
	private String name = "name";
	private String id = "id";
	private String target = null;
	private String img = "false";
	private String dir = null;
	
	private static final String TAG = "[tagFTP]";
	private static final MkLogger mklogger = new MkLogger(TAG);
	//Log ?���?
	public void setObj(String obj) {	this.obj = obj;	}
	public void setName(String name) {	this.name = name;	}
	public void setId(String id) {	this.id = id;	}
	public void setImg(String img) {	this.img = img;	}
	public void setTarget(String target) {	this.target = target;	}
	public void setDir(String dir) {	this.dir = dir;	}
	
	private ArrayList<MkPageJsonData> getPageControl(HttpServletRequest request) {
		Object o = request.getAttribute("mkPage");
		if(o == null) {	return null;	}

		String controlName = o.toString();
		return MkPageConfigs.Me().getControl(controlName);
	}

	private ArrayList<MkFtpData> getFtpControl(String ftpControlName){
		return MkFTPConfigs.Me().getControl(ftpControlName);
	}

	public void doTag() throws JspException, IOException{
		
		HttpServletRequest request = (HttpServletRequest) ((PageContext)getJspContext()).getRequest();

		HttpServletResponse response = (HttpServletResponse) ((PageContext)getJspContext()).getResponse();
		
		request.setAttribute("passed-ftp", "Hello");
		request.setCharacterEncoding("UTF-8");

		ArrayList<MkPageJsonData> pageInfo = getPageControl(request);
		ArrayList<MkFtpData> ftpInfo = getFtpControl(this.name);

		boolean isSet = (pageInfo == null || pageInfo.size() == 0) ? false : true;
		MkPageJsonData pageStaticData = null;

		if(isSet) {
			for(int i = 0; i < pageInfo.size(); i++) {
				if(pageInfo.get(i).getPageStatic()) {
					pageStaticData = pageInfo.get(i);
					break;
				}
			}
		}
		
		int pageServiceIndex = -1;
		boolean pageServiceFound = false;
		for(MkPageJsonData pjd : pageInfo) {
			pageServiceIndex++;
			if(this.id.contentEquals(pjd.getServiceName())) {
				pageServiceFound = true;
				break;
			}
		}
		if(!pageServiceFound) {
			pageServiceIndex = -1;
			mklogger.error(" Tag 'id(" + this.id + ")' is not matched with any page service 'type:id'.");
			return;
		}
		
		int ftpServiceIndex = -1;
		boolean ftpServiceFound = false;
		for(MkFtpData fjd : ftpInfo) {
			ftpServiceIndex++;
			if(this.id.contentEquals(fjd.getServiceName())) {
				ftpServiceFound = true;
				break;
			}
		}
		if(!ftpServiceFound) {
			ftpServiceIndex = -1;
			mklogger.error(" Tag 'id(" + this.id + ")' is not matched with any ftp service 'type:id'.");
			return;
		}
		
		MkFtpData ftpService = ftpInfo.get(ftpServiceIndex);
		
		String filePath = ftpService.getPath();
		/*
		 * 똑같이 여기서도 prefix 달아주고 hash 해주면 됨
		 */
		String ftpDirPrefix = ftpService.getDirPrefix();
		String[] dirs = null;
		boolean ftpDirHash = ftpService.getHashDirPrefix();
		
		mklogger.debug("dir : " + this.dir + "/");
		if(ftpDirPrefix != null) {
			if(this.dir != null)
				ftpDirPrefix = this.dir;
			
			if(ftpDirPrefix.contains("^")) {
				dirs = ftpDirPrefix.split("\\^");
				String tempDir = "";
				for(String dir : dirs) {
					if(ftpDirHash)
						tempDir += "/" + MkCrypto.MD5(dir + "__TRIP_!!_DIARY__");
					else
						tempDir += "/" + dir;
				}
				
				ftpDirPrefix = tempDir;
			}else {
				if(ftpDirHash) 
					ftpDirPrefix = "/" + MkCrypto.MD5(ftpDirPrefix + "__TRIP_!!_DIARY__");
				else
					ftpDirPrefix = "/" + ftpDirPrefix;
			}
		}else {
			ftpDirPrefix = "";
		}
		mklogger.debug("ftDirPrefix : " + ftpDirPrefix);
		filePath = filePath + ftpDirPrefix;

		/*
		if(!(MkConfigReader.Me().get("mkweb.ftp.absolute").contentEquals("yes")))
			filePath = MkFTPConfigs.Me().getPrefix() + filePath; 
*/
		mklogger.debug("tag filePath : " + filePath);
		
		/*
		 * 똑같이 여기서도 prefix 달아주고 hash 해주면 됨
		 */
		
		//
		File[] ftpResults = new File(filePath).listFiles();
		ArrayList<Object> resultObject = new ArrayList<>();
		
		if(ftpResults != null) {
			boolean selectAll = (this.target == null);
			for(File result : ftpResults) {

				String fileName = result.getName();
				String source = ftpService.getPath() + ftpDirPrefix + "/" + fileName;
				String[] extensions = fileName.split("\\.");
				String extension = extensions[extensions.length-1];
				LinkedHashMap<String, String> ftpResult = null;
				if(!selectAll) {
					if(fileName.contentEquals(this.target)) {
						ftpResult = new LinkedHashMap<>();
						
						ftpResult.put("name", fileName);
						ftpResult.put("result", source);
						resultObject.add(ftpResult);
					}
				} else {
					ftpResult = new LinkedHashMap<>();
					
					ftpResult.put("name", fileName);
					ftpResult.put("result", source);
					resultObject.add(ftpResult);
				}
				/*
				if(img.contentEquals("yes")) {
					if(!selectAll) {
						if(fileName.contentEquals(this.target)) {
							ServletContext sc = request.getServletContext();
							resultObject.add(readImageFromStream(sc, source, fileName, extension));
						}
					} else {
						ServletContext sc = request.getServletContext();
						resultObject.add(readImageFromStream(sc, source, fileName, extension));
					}
				}else {
					if(!selectAll) {
						if(result.getName().contentEquals(this.target)) {
							LinkedHashMap<String, String> ftpResult = new LinkedHashMap<>();
							
							ftpResult.put("name", fileName);
							ftpResult.put("result", source);
							resultObject.add(ftpResult);
						}
					}else{
						LinkedHashMap<String, String> ftpResult = new LinkedHashMap<>();
						
						ftpResult.put("name", fileName);
						ftpResult.put("result", source);
						resultObject.add(ftpResult);
					}
				}
				*/
			}
		}
		
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		
		if(resultObject != null && resultObject.size() > 0)
		{
			for(int i = 0; i < resultObject.size(); i++)
			{
				result = (LinkedHashMap<String, Object>) resultObject.get(i);
				((PageContext)getJspContext()).getRequest().setAttribute("mkw", result);
				getJspBody().invoke(null);
			}
			((PageContext)getJspContext()).getRequest().removeAttribute("mkw");
		}else {
			return;
		}
		
	}
	
	private LinkedHashMap<String, String> readImageFromStream(ServletContext sc, String source, String fileName, String extension) throws IOException{
		LinkedHashMap<String, String> result = null;
		try(InputStream is = sc.getResourceAsStream(source)){
			result = new LinkedHashMap<>();
			BufferedImage bufferedImage = ImageIO.read(is);
			String tempName = (System.currentTimeMillis() + extension);
			File tmpFile = new File(tempName);
			
			ImageIO.write(bufferedImage, extension, tmpFile);
			byte[] imageBytes = Files.readAllBytes(tmpFile.toPath());
			
			Base64.Encoder encoder = Base64.getEncoder();
			String encoding = "data:image/"+extension+";base64," + encoder.encodeToString(imageBytes);
			
			result.put("name", fileName);
			result.put("result", encoding);
			
			tmpFile.delete();
		}
		return result;
	}
}
