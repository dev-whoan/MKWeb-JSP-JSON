
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