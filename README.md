# character-encoding-filter
=========================
This is a filter that solve GET form or POST form encoding problem.

## Usage
First put the CharacterEncodingFilter.jar to the WEB-INF/lib directory
then add these codes to WEB-INF/web.xml
```
<filter>
	<filter-name>character-encoding-filter</filter-name>
	<filter-class>com.github.zhanhb.filter.CharacterEncodingFilter</filter-class>
	<init-param>
		<description>Encoding for content and query string, default UTF-8.</description>
		<param-name>characterEncoding</param-name>
		<param-value>UTF-8</param-value>
	</init-param>
</filter>
<filter-mapping>
	<filter-name>character-encoding-filter</filter-name>
	<url-pattern>/*</url-pattern>
</filter-mapping>
```

or if your encoding is UTF-8
you can config your web.xml more simple.
```
<filter>
	<filter-name>character-encoding-filter</filter-name>
	<filter-class>com.github.zhanhb.filter.CharacterEncodingFilter</filter-class>
</filter>
<filter-mapping>
	<filter-name>character-encoding-filter</filter-name>
	<url-pattern>/*</url-pattern>
</filter-mapping>
```

Maven Clone:
```
<dependency>
  <groupId>com.github.zhanhb</groupId>
  <artifactId>character-encoding-filter</artifactId>
  <version>2.1.6.3</version>
</dependency>
```
