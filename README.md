
<img src="https://user-images.githubusercontent.com/65178775/83269009-6a481f80-a201-11ea-8475-0a779375a005.png" width="100%" />

# MKweb
Minwhoan - Kihyeon's JSP-Servlet Web Server Framework Repository

# MkWeb wiki
https://github.com/dev-whoan/MKWeb/wiki
OR
https://mkweb.dev-whoan.xyz

# Download Without Source
<a href="https://mkweb.dev-whoan.xyz/deploy.zip" target="_blank">Download</a>

# What is MKWeb?

MkWeb is well created 'Web-Server' framework.

We considered that developers should consider lots of things when he/she started to create some services; App, WebSite, science experimental, etc. Simply Front-End, and Back-End.

But actually, the simple service idea is about Front-End like: How about creating a food delivery service? People can choose the foods what they want to eat, and the page will show information about the food. Furthermore when they order it, the seller will receive the infor where consumer wants to receive it, and the other things about infor.

So the idea started with 'Developers just need to focus on the Front-End, easily View-side.'

However, there are lots of libraries for creating Web-Server, but when developer use it, he/she needs to construct, code, and test about it. It costs time too much, so we thought that 'What about let we solve the web-server part?'

So MKWeb borned.

We are designing our MKWeb with MVC pattern( however we are students and not pertty good at desining it, but we are working hard on how can we follow the pattern. ), and what MKWeb should offer to let developers focusing into there 'Front-End'.

<p align="center">
<img src="https://user-images.githubusercontent.com/65178775/81583650-9b94b300-93ec-11ea-8683-c4ffc67215f9.png" width="66%"/>
</p>
* Model

A server-side resources such as; DbAccessor, FileTransfer, Logger, ...

* Controller

A bridge of Model <---> Views

* Service

The actual functions(workers) which included in Controller

# JSON Configs
* Controller

In MkWeb, controller means all the relations and actions needed to define the relationship between Model and View such as page configs, sql configs, and etc, that 

Definition of Services and Controllers

JSON Configs are the biggest profit  when you use MKWeb.

You can easily access to Model via services or controllers.
To use the services and controllers, you <span>need to set the config files, ~.json</span>.
<br><br>
The configs are designed with <span>JSON</span>, so you can <span>simply define it</span>.
<br><br>
For example, before using MKWeb you need to design DBA, DBO, and whatever you need to access to DB, <span>but now you just need to set relevant Configs to access DB.</span>
<br><br>
Define the <span>SQL Json</span>, <span>and View Json</span> to choose which query to use, and after defining it, you can easily execute the SQL.
<br><br>
Just you need to define the relations <span>well</span> for request to response.
<br><br>
If the relations or requests are not defined, MkLogger would let you know what's going on.
<br><br>
</p>

<br>
<span class='h1'>What MkWeb Can Do?</span>
<hr>

<p>
<br>
<span class='h2'>Logging, DB Connect, and RESTful API</span>
<br>
MKWeb supports Logging, DB connect, and limited RESTful API functions.
<br><br>
MkLogger is logging every tasks on MkWeb, so you can check which requests has come, and what was the response.
<br>
You can manage your webserver easily with MkLogger's feedback.
<br>
For example, if the user asked wrong requesets, MkLogger would tell what is the problem,<br>
And user asked right requests, and you set wrong definition(or relations) on controller, MkLogger would also tell the problem.<br>
So, <span>easy maintance</span>.<br>
Also you can use MkLogger on your custom java file so you can check your custom log on MkLogger.

<br><br>
With other frameworks, you need to connect DB with programming it, and create every DAO, DTO, Data Beans.
<br>
However, using mkweb, <span>you don't have to create any DAO, DTO, and Data Beans.</span>
<br>
You <span>just need to config the View jsons and SQL jsons.</span>
<br>
You can easily use your query result data with &lt;mkw:get&gt; HTML Custom Tag.
<br><br>
MkWeb supports RESTful API.
<br>
For now, it's not 100% developed, but we are planning to support every function in RESTful API.
<br><br>
Supporting functions: <span>Method: Get, Post</span>
<br><br>
However, Get method is now limited supported. Following is now supporting functions.
<br>
For example, if you have Users data set, and there are 3 columns; name, phone, and address:
<br><span>
1. /users<br>
2. /users/name/이름<br>
3. /users/name/이름/phone/번호<br>
</span> ==> 1. searching everything in users 2. searching with perfect condition (name have value, and phone have value)<br>

And following functions are not supporting now.
<br><br><span>
1. /users/name<br>
2. /users/name/이름/phone
</span> ==> 1. want to get only name column that includes whole names in users / 2. want to get only phone column that the people whos' name is John.)
<br><br>
We support 100% POST method to create new data.<br>
But there are some rules to use POST method, you can check it on <a href="./conf_api.html">RESTful API Configs</a>.
<br><br>
<span class='h2'>What We Are Planning To</span>
<br>
We are planning to following functions.
<br><br>
1. 100% RESTful API<br>
2. Session<br>
3. Device informations(Header)<br>
</p>
<span class='h1'>Developer E-mail</span>
<hr>
<p style="text-align: center;">
<span class='h2'>dev-whoan</span>
dev.whoan@gmail.com
<br><br>
<span class='h2'>hyeonic</span>
evan3566@gmail.com
<span class='h2'>koh</span>
khj1538104@gmail.com

# What we want you to do when you use MKWeb or copy and distribute MKWeb(convey).
* MKWeb을 사용하거나 복제, 배포할 때 해줬으면 하는 일

1. Don't forget who created it.
- Minwhoan, Kihyeon. We just wanted to be well known programmers.

2. Please scout us.
