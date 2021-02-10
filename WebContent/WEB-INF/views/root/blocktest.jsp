
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="MkWeb" prefix="mkw"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>MKWEB_TEST</title>
<script
	src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.0/jquery.min.js"></script>
<script>
$(document).ready(function(){
	$("#testBlock").click(function(){
		let jsonInfo = '{"title":"테스트", "subjectName":"테스트 프로그래밍", "subjectCode":"0011", "level":"3"}';
		$.ajax({
	        type : "POST",
	        url : "http://localhost:8080/kuchain/test",
	        dataType : "json",
			data : {
	        	"badgesaction":jsonInfo
	        },
	        error : function(a, b, c){
	            alert("통신실패!!!!");
	            console.log(a.responseText);
	            console.log(b);
	            console.log(c);
	        },
	        success : function(rd){
	        	console.log(rd);
	        	let tempString, tempJSON;
	        	tempString = JSON.stringify(rd);
	        	tempJSON = JSON.parse(tempString);
	        	userList = tempJSON['users'];
	        	getLastSequence();
	        }
	    });
	});
});
</script>
<style>
#remove-user {
	display: none;
}

.show {
	display: block !important;
}
</style>


</head>
<body>
	<div>

		<div class="wrap">
			<!-- header -->
			<div class="header">Default Upload Page</div>

			<!-- section -->
			<div class="section">
				<div class="container">
					<h1 id="title">MKWeb Test FTP - Upload</h1>
					<button id="testBlock">제출</button>
				</div>
			</div>

			<!-- footer -->
			<div class="footer"></div>
		</div>
	</div>
</body>
</html>

</html>
