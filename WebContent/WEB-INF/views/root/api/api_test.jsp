 
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="MkWeb" prefix="mkw" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Insert title here</title>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.0/jquery.min.js"></script>
<link rel="stylesheet" href="./css/mystyle.css" />
<script>

var user_seq;

window.onload = function(){
	let get = document.getElementsByClassName("one-item");
	console.log(get);
	if(!get){
		console.log("데이터 없음");
		let tbody = document.getElementById('table-wrapper');
		
		if(tbody){
			tbody.innerHTML = "<tr> <td>없음</td> <td> 없음</td> </tr>";
		}
	}
};

$(document).ready(function(){
	$("#test-api").click(function(){
		var jsonInfo = '{"search_key":"apple", "name":"dev.whoan"}';
		var queryInfo = "search_key=apple&name=dev.whoan";
		$.ajax({
	        type : "post",
	        url : "/mk_api_key/users",
	        dataType : "json",
	        //apiData : {"search_key":"apple", "person" : {"name":"Eugene","age":24}} 
	        data : {
	        	"apiData":queryInfo
	        },
	        error : function(){
	            alert("통신실패!!!!");
	        },
	        success : function(rd){
	            console.log(rd);
	        }
	    });
	});
});

</script>
<style>
#modify-user{
	display: none;
}

#remove-user{
	display: none;
}

.show{
	display: block !important;
}
</style>


</head>
<body>
This is api test page

<div>

	<div class="wrap">
        <!-- header -->
        <div class="header">
        </div>
 
        <!-- section -->
        <div class="section">
            <div>
            	<button id="test-api">API 확인</button>
            </div>
        </div>

        <!-- footer -->
        <div class="footer">

        </div>
    </div>
</div>
</body>
</html>
