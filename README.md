
<img src="https://user-images.githubusercontent.com/65178775/83269009-6a481f80-a201-11ea-8475-0a779375a005.png" width="100%" />

#This version use JSON

# MKweb
Minwhoan - Kihyeon, and Koh's JSP-Servlet Web Server Framework Repository

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
- Definition of Services or Controllers.
The biggest profit when you use MKWeb.

You can access to Model with service or controller configs.

We designed the configs with JSON, so developer can easily access to Server resources.

Before using MKWeb, you need to design DBA, DBO, and whatever you need to access to DB, but now you just need to set Config files to access DB.

For example, you just need to set SQL config, and Page config ( We will introduce about it below part or MKWeb Wiki. )

The response depends on what view requested, however you just need to define the response for the request on Configs.

If the request is not defined, then response will call error-page.

# What We Are Planning to

1. RESTful API

2. Session

# Creator
Minwhoan, Kihyeon, and Koh
- who just want to be good and well known programmers.

