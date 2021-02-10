 
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="MkWeb" prefix="mkw" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>MKWEB_TEST</title>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.0/jquery.min.js"></script>
<link rel="stylesheet" href="./css/mystyle.css" />
<script>

$(document).ready(function(){
	let get = document.getElementsByClassName("one-item");
	console.log(get);
	if(!get){
		console.log("No data");
		let tbody = document.getElementById('table-wrapper');
		
		if(tbody){
			tbody.innerHTML = "<tr> <td>No</td> <td>data</td> </tr>";
		}
	}
	
	$("#textAJAX").click(function(){
		$.ajax({
	        type : "POST",
	        url : "/data/receive",
	        dataType : "text",
	        data : {
	        	"iu.user_name" : $("#Name").val(),
	        	"iu.user_class" : $("#Info").val(),
	        	"iu.user_ip" : "IP_INPUT"
	        },
	        error : function(a, b, c){
	            console.log(a + "\n" + b + "\n" + c);
	        },
	        success : function(rd){
	            console.log(rd);
	        }
	         
	    });
	});
	
	$("#changeAJAX").click(function(){
		$.ajax({
	        type : "POST",
	        url : "/data/receive",
	        dataType : "text",
	        data : {
	        	"cu.user_name" : $("#newName").val(),
	        	"cu.old_name" : $("#oldName").val()
	        },
	        error : function(a, b, c){
	            console.log(a + "\n" + b + "\n" + c);
	        },
	        success : function(rd){
	            console.log(rd);
	        }
	         
	    });
	});
});
</script>
<style>

#remove-user{
	display: none;
}

.show{
	display: block !important;
}
</style>


</head>
<body>
<div>

	<div class="wrap">
        <!-- header -->
        <div class="header">
        Default Page
        </div>
 
        <!-- section -->
        <div class="section">
            <div class="container">
                <h1 id="title">MKWeb Get User Page Static By Name</h1>
                <div id="search_name">
                    <form action="" method="post">
                        <!-- <label>이름 : </label> -->
                        <input type="text" name="id">
                        <input type="password" name="password">
                        <input type="submit" value="Search By Class">
                    </form>
                </div>
            </div>
        </div>

        <!-- footer -->
        <div class="footer">

        </div>
    </div>
</div>
</body>
</html>
