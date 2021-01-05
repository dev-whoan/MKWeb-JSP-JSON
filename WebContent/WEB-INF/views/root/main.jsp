 
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
<div>

	<div class="wrap">
        <!-- header -->
        <div class="header">
        </div>
 
        <!-- section -->
        <div class="section">
            <div class="container">
                <h1 id="title">MKWeb Get User Page Static By Name</h1>
                <table id="user_info_list"> 
                    <thead>
                        <tr>
                            <th>name</th>
                            <th>address</th>
                            <th>비고</th>
                        </tr>
                    </thead>
                    <tbody id="table-wrapper">
                    	<mkw:get name="user" id="selectName" obj="list" like="no">
                    		<tr>
	                    		<th style="font-weight: 300">${mkw.name}</th>
	                    		<th></th>
                    		</tr>
                    	</mkw:get>
                    	
                    </tbody>
                </table>
                
                <h1 id="title">MKWeb Get User Page Static By Class</h1>
                <table id="user_info_list"> 
                    <thead>
                        <tr>
                            <th>name</th>
                            <th>address</th>
                            <th>비고</th>
                        </tr>
                    </thead>
                    <tbody id="table-wrapper">
                    	<mkw:get name="user" id="selectClass" obj="list" like="no">
                    		<tr>
	                    		<th style="font-weight: 300">${mkw.u_class}</th>
	                    		<th></th>
                    		</tr>
                    	</mkw:get>
                    </tbody>
                </table>
                
                <h1 id="title">MKWeb Get User By Class</h1>
                <table id="user_info_list">
                    <thead>
                        <tr>
                            <th>name</th>
                            <th>address</th>
                            <th>비고</th>
                        </tr>
                    </thead>
                    <tbody id="table-wrapper">
                    	<mkw:get name="user" id="selectUserByClass" obj="list" like="no">
                    		<tr>
	                    		<th style="font-weight: 300">${mkw.name}</th>
	                    		<th style="font-weight: 300">${mkw.u_class}</th>
	                    		<th></th>
                    		</tr>
                    	</mkw:get>
                    </tbody>
                </table>

                <div id="search_name">
                    <form action="" method="post">
                        <!-- <label>이름 : </label> -->
                        <input type="text" name="byclass.user_class">
                        <input type="submit" value="Search By Class">
                    </form>
                </div>
                
                <div id="search-class-seq">
                	<form action="" method="post">
						<label for="ucs.user_seq">SEQ</label>
                		<input type="text" name="ucs.user_seq" />
						<label for="ucs.user_class">Class</label>
                		<input type="text" name="ucs.user_class" />
                		<input type="submit" value="Search" />
                	</form>
                </div>
                
                <div id="modify-user" style="display: block">
					<label for="ucs.name">Name</label>
                	<input type="text" id="Name" name="name" />
					<label for="ucs.class">Class</label>
	                <input type="text" id="Info" name="class"/>
	                <button id="textAJAX">Insert</button>
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
