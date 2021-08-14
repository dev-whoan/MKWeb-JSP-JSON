
# 08/14/2021 (KST) 0.1.1 Updated

1. MkAuthToken bug fixed

- Now encode token without padding.

- Use HS256 as default if no algorithm or invalid algorithm have set.

# 08/12/2021 (KST) 0.1.0 Updated

1. MkRestApi bug fixed

2. MkAuthToken have added

- Page, SQL, FTP now have auth for it

3. MkFileTransfer bug fixed

# 02/23/2021 (KST) 0.0.8a updated

1. MkFileTransfer now supports remove mode.

- FTP service, there is a new attribute "type" which have values : ["ftp-receiver", "ftp-remover"].

- As its' type, one for receiving files, and another for removing files.

- Default ftp.uri changed into /ftp/execute

2. Preparing Features

- Supports MkLogger for logging each controller's debug level.

- For this update, MkLogger is no longer singleton designed.

# 02/22/2021 (KST) 0.0.7 updated

1. Mk RESTful API updated

- MkWeb.conf property added : mkweb.restapi.refonly.host

- Which set allow CORS or not. If yes, only RESTful API accessed via "mkweb.web.hostname".

# 02/21/2021 (KST) 0.0.7 updated

1. Bug fix

- File Receiver Error Fixed.
MkFileTransfer couldn't get service by uri, but now fixed. 

# 02/17/2021 (KST) 0.0.7 commit

1. Bug fix

- Device Controller Error Fixed. ( For unsupported device, couldn't reach to right uri which is not same as the default language, but now it does. It was reached to default language page of the device.) 

2. Some attributes of SQL Controller have changed.

- Attribute of "table" have changed into JSONObject to support JOIN. There is a "join" attribute inside of "table", however you can ignore writing "join" attribute if you don't use.

- Every attribute must be written in lower case. If you don't, it can cause some errors.

- Example: "table":{ "from":"User" } OR

<pre>

"table":{
	"from":"User",
	"join":{
		"type":"INNER",
		"joinfrom":"Place",
		"on":""
	}
},

</pre>

# 02/10/2021 (KST) 0.0.6 commit

1. SQL service on RESTful API, you can add essential parameters.

- For example, if you want let client request at least one essential parameter, use this parameter.

- If you don't want to use it, please let it "1":"*"

<pre>

{
	"Controller": {
		"name":"api_user",
		"debug":"error",
		"db":"mkwiki",
		"table":"User",
		"api":"yes",
		"condition":{
			"1":"name",
			"2":"u_class",
			"3":"SEQ",
			"4":"CNT_IP"
		},
	<span>
			
		"parameter":{
			"1":"*"
		},
	</span>

</pre>
		

# 02/10/2021 (KST) 0.0.6 Commit

1. RESTful API allowed to search in custom Table.

- For example, if you create table for each users, you need to create every api.views for the new table.

- Now, you can use mkweb.restapi.search.customtable to allow execute on customTable as a parameter.

- example: curl --request GET "https://test-mkweb.dev-whoan.xyz/mk_api_key/users/name/dev.whoan?serach_key=openkey&customTable=User"

2. FTP service now can have dynamic directory.

- For example, if you create ftp server for each user or each requestes, you had to use only fixed dir.

- Now, you can request with dynamic directory by mkw:ftp tag, attribute 'dir'.

- You need to set directory inside of 'ftp controller' :

3. RESTful API Bug fixed

- When error occured while GET method on DBA, it returned 204. Now returns the origin error. 

- Error was didn't exit the method right after error have occured.

<pre>

{
	"Controller": {
		"name":"freeboard",
		"path":"/mkweb/board",
		"debug":"error",
		"services":[
			{
				"id":"Image",
				"servicepath":"/img",
				"dir":"user_prefix",		/* this is a request parameter name. you can send dynamic directory by this parameter */
				"hash_dir":"true",			/* use hash dir or not. only requested dir will be hashed. Now, MD5 is used. */
				"format":{
					"1":"jpg",
					"2":"png",
					"3":"gif",
					"4":"PNG"
				}
			}
		]
	}
}


/* But also you need to set on view controller */
{
	"Controller": {
		"name":"ftp-uploader",
		"last_uri":"upload",
		"device":{
			"desktop":{
				"default":{
					"path":"/views/root",
					"file":"upload.jsp",
					"uri":""
				}
			}
		},
		"debug":"error",
		"api":"no",
		"services":[
			{
				"page_static":"false",
				"type":{
					"kind":"ftp",
					"id":"Image"
				},
				"method":"post",
				"obj":"list",
				"parameter_name":"ftp_img",
				"value":{
					"1":"testimg",
					"2":"user_prefix"					/* Right here. Must same with ftp controllers' dir attribute */
				}
			},
			{
				"page_static":"false",
				"type":{
					"kind":"ftp",
					"id":"archieve"
				},
				"method":"post",
				"obj":"list",
				"parameter_name":"ftp_zip",
				"value":{
					"1":"testzip"
				}
			}
		]
	}
}

</pre>