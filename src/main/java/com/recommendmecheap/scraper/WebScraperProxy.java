package com.recommendmecheap.scraper;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.*;
import java.net.Socket;
import java.net.Proxy;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.io.*;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.*;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.kafka.clients.admin.internals.DescribeProducersHandler;

public class WebScraperProxy {

	public static final String userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/37.0.2062.94 Chrome/37.0.2062.94 Safari/537.36";
	public static final String proxyFileLocation = "/tmp/proxies.txt"; //TODO make location viable everywhere

	private static final Logger logger = LogManager.getLogger(WebScraperProxy.class);
	private static final InetAddressValidator ipValidator
                              = InetAddressValidator.getInstance();
	private static final int NumberOfIpsToScrape = 10;


	private final String proxyType = "";
	private int proxyPointer;
	private final WebScraperProxyBuilder builder;
	private ArrayList<EProxy> proxyWebsites;
	private ArrayList<String> knownWebsites;
	private EProxy checkedProxy;

	public WebScraperProxy(WebScraperProxyBuilder builder) {
		this.builder = builder;
		this.checkedProxy = new EProxy();
		this.proxyWebsites = new ArrayList<>();
		this.proxyPointer = 0;
	}

	public void Init()
	{
		try {
			//Get all scraped proxies in case sth goes wrong with trustableProxy
			this.knownWebsites = LoadKnownWebsites();
			ArrayList<EProxy> tmp = ArrayList<>();
			if(!ProxyWebsitesAlreadyScraped())
			{
				for (String website : this.knownWebsites) {
					tmp = GetProxyWebsites(""); //Pass query param as null
					this.proxyWebsites = Stream.concat(this.proxyWebsites.stream(), tmp.stream()).toList();
				}
				StreamProxyListToFile(this.proxyWebsites); //Cache the proxies
			} else {
				this.proxyWebsites = LoadScrapedProxyWebsites();
			}
			logger.info("Number of proxy websites found(unchecked)" + this.proxyWebsites.size());
			if(this.builder.context.proxyCache.IsExists()) //Load if checked proxy already exists
			{
				logger.info("Loading existing proxy cache!");
				this.checkedProxy = this.builder.context.proxyCache.Load();
				logger.info(this.checkedProxy.toString());
				if (!EProxy.IsValid(this.checkedProxy)) //In case the loaded proxy is not valid
				{
					logger.info("Cached proxy is not valid, advising a new one!");
					adviseAProxy();
				}
			}
			else
			{
				logger.info("Cannot find a cached proxy");
				adviseAProxy();
			}
			logger.info("Checked proxy " + this.checkedProxy.toString());
		} catch (Exception ex) {
			logger.error("Error initializing the proxy engine " + ex.toString());
		}
		logger.info("Advised proxy..." + this.checkedProxy.toString());
	}

	public void Scrape(String site, boolean streamToFile)
	{
		Document result = null;
		while(result == null){
			logger.info("Advised proxy..." + this.checkedProxy.toString());
			result = MakeProxyRequest(site);
			if(result == null)
			{
				//If we return an invalid proxy, invalidate the old one and return a new one
				adviseAProxy();
			}
		}
		if(streamToFile)
			this.builder.context.documentCache.Write(result);
	}

	public Document MakeProxyRequest(String site) 
	{
		HttpURLConnection con = null;
		Document doc = null;
		String proxySite = this.checkedProxy.Ip;
		int port = this.checkedProxy.Port;
		try {
			URL toUrl = new URL(site);

			Proxy proxyClient = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxySite, port));

			logger.info("Proxy Client - " + proxyClient.toString());
			
			logger.info(toUrl.toString());
			con = (HttpURLConnection) toUrl.openConnection(proxyClient);

			//con.setRequestProperty("Accept-Encoding", "gzip, deflate");
			//con.setRequestProperty("Content-Type", "text/html; charset=utf-8");
			con.setRequestProperty("User-Agent", WebScraperProxy.CreateRandomUserAgent("user-agent.txt"));
			con.setConnectTimeout(100);
			con.connect();
			con.setConnectTimeout(100);
			con.setReadTimeout(500);
			int status = con.getResponseCode();
			if(status != 200)
			{
				throw new Exception("Response code is : " + status + "\n" + "Response" + con.getHeaderFields().toString());
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuffer content = new StringBuffer();
			String inputLine;
			while((inputLine = in.readLine()) != null){
				content.append(inputLine);
			}
			in.close();
			con.disconnect();
			doc = Jsoup.parse(content.toString()); //Take tables
			return doc;
		} catch (Exception e) {
			logger.error(e.toString());
			con.disconnect();
			return doc; 
		}
	}

	public static boolean Check(String proxySite, int port, String to)
	{
		try 
		{
			URL toUrl = new URL(to);
			//String proxy = proxyType + "://" + value;
			Proxy proxyClient = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxySite, port));

			logger.debug("Proxy Client - " + proxyClient.toString());

			HttpURLConnection socketConnection = (HttpURLConnection) toUrl.openConnection(proxyClient);

			socketConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			socketConnection.setRequestProperty("User-Agent", WebScraperProxy.CreateRandomUserAgent("user-agent.txt"));
			socketConnection.setConnectTimeout(100);
			socketConnection.setReadTimeout(100);
			socketConnection.connect();

			logger.debug("SocketConnection - " + socketConnection.toString());

			logger.debug("SocketConnection response msg - " + socketConnection.getResponseMessage());

			socketConnection.disconnect();
			return true;
		} catch(Exception ex) {
			logger.debug("Cannot check proxy - " + ex.toString());
			return false;
		}
	}

	public static WebScraperProxyBuilder GetProxyBuilder()
	{
		return new WebScraperProxyBuilder();
	}

	public static String CreateRandomUserAgent(String file)
	{
		try {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			InputStream inputStream = classloader.getResourceAsStream(file);
			InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(streamReader);

			List<String> lines = new ArrayList<String>();

			String result = "";
			
			String line = "";
			while ( (line = reader.readLine()) != null)
			{
				lines.add(line);
			}

			int random = ThreadLocalRandom.current().nextInt(0, lines.size());

			result = lines.get(random);

			if(result == "")
			{
				throw new Exception("Cannot retrieve the result");
			}

			reader.close();

			return result.replace("\n", "");

		} catch(Exception ex)
		{
			logger.error("Cannot create a random user agent - " + ex.toString() + ". Therefore, using the default one...");
			return WebScraperProxy.userAgent;
		}
	}
	public ArrayList<EProxy> GetProxyWebsites(String query)
	{
		ArrayList<EProxy> result = new ArrayList<>();
		try {
			String scrapeResult = ScrapeProxyWebsites().toString();	
			result = parseScrapeListHtml(scrapeResult, query);
		} catch (Exception e) {
			//TODO: handle exception
			logger.error(e.toString());
			System.exit(1);
		}
		return result;
	}
	public void adviseAProxy()
	{
		try {
			this.builder.context.proxyCache.Invalidate();
			for(int counter = this.proxyPointer + 1; counter < this.proxyWebsites.size(); counter++)
			{
					EProxy i = this.proxyWebsites.get(counter);
					logger.info(String.format("Scraped proxy for IP %s and Port %d !!!", i.Ip, i.Port));
					if(WebScraperProxy.Check(i.Ip, i.Port ,"https://google.com"))
					{
							logger.info(String.format("Scraped proxy works for IP %s and Port %d !!!", i.Ip, i.Port));
							this.checkedProxy.Ip = i.Ip;
							this.checkedProxy.Port = i.Port;
							this.checkedProxy.Counter = counter;
							this.proxyPointer = counter;
							break;
					}
			}
			logger.info(String.format("Scraped proxy for IP %s and Port %d !!!", this.checkedProxy.Ip, this.checkedProxy.Port));
			this.builder.context.proxyCache.Write(this.checkedProxy);
		} catch (Exception e) {
			//TODO Handle advice gone wrong
		}
	}
	public static ArrayList<EProxy> LoadScrapedProxyWebsites()
	{
		try {
			BufferedReader reader = new BufferedReader(new FileReader(WebScraperProxy.proxyFileLocation));
			ArrayList<EProxy> result = new ArrayList<>();
			String line = "";
			while((line = reader.readLine()) != null)
			{
				String[] data = line.split(",");
				EProxy entry = new EProxy();
				entry.Ip = data[0];
				entry.Port = Integer.parseInt(data[1]);
				result.add(entry);
			}
			reader.close();
			return result;
		} catch (Exception e) {
			System.exit(1);
		}
		return null;
	}
	public boolean ProxyWebsitesAlreadyScraped()
	{
		File file = new File(WebScraperProxy.proxyFileLocation);
		return file.exists();
	}
	public static void StreamProxyListToFile(ArrayList<EProxy> in)
	{
		try {
			FileWriter writer = new FileWriter(WebScraperProxy.proxyFileLocation);
			for(EProxy s : in)
			{
				writer.write(s.Ip + "," + s.Port + "\n");
			}
			writer.close();
		} catch (Exception e) {
			//TODO: handle exception
			System.exit(1);
		}
	}
	private StringBuffer ScrapeProxyWebsites() 
	{
		try {
			HttpURLConnection con = (HttpURLConnection) builder.getProxyUrl().openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(1000);
			con.setReadTimeout(1000);
			int status = con.getResponseCode();
			if(status != 200)
			{
				throw new Exception("Response code is : " + status + "\n" + "Response" + con.getHeaderFields().toString());
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			StringBuffer content = new StringBuffer();
			String inputLine;
			while((inputLine = in.readLine()) != null){
				content.append(inputLine);
			}
			in.close();
			con.disconnect();
			return content;
		} catch (Exception e) {
			logger.error(e.toString());
			return new StringBuffer();
		}
	}

	private List<String> LoadKnownWebsites()
	{
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			InputStream inputStream = classloader.getResourceAsStream("knownWebsites.txt");
			InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
			BufferedReader reader = new BufferedReader(streamReader);

			List<String> lines = new ArrayList<String>();

			String result = "";
			
			String line = "";
			while ( (line = reader.readLine()) != null)
			{
				lines.add(line);
			}
			return lines;
	}

	private ArrayList<EProxy> parseScrapeListHtml(String in, String query)
	{
		Document doc = Jsoup.parse(in); //Take tables
		Elements rows = doc.select("tr");
		ArrayList<EProxy> result = new ArrayList<>();
		for(Element row: rows)
		{
			try{
				String [] parts = row.text().split(" ");
				EProxy entry = new EProxy();
				if(ipValidator.isValidInet4Address(parts[0]))
				{
					logger.debug("Found valid ip");
					entry.Ip = parts[0];
				}
				if(1 <= Integer.parseInt(parts[1]) && Integer.parseInt(parts[1])<= 65535)
				{
					entry.Port = Integer.parseInt(parts[1]);
				}
				if(!EProxy.IsValid(entry))
				{
					break;
				}
				result.add(entry);
			} catch(Exception ex) {
				continue;
			}
		}
		return result;
	}
	public static class EProxy {
		String Ip;
		int Port;
		String Description;
		int Counter;
		EProxy()
		{
			this.Ip = "";
			this.Port = 0;
			this.Description = "";
			this.Counter = 0;
		}
		EProxy(String ip, int port, String description, int counter)
		{
			this.Ip = ip;
			this.Port = port;
			this.Description = description;
			this.Counter = counter;
		}
		@Override
		public String toString()
		{
			return "Ip: " + this.Ip + " Port: " + this.Port + " Description: " + this.Description + " Counter: " + this.Counter;
		}
		public static boolean IsValid(EProxy p )
		{
			return (p != null) && !(p.Ip == null || p.Ip == "" || p.Ip == " ") && !(p.Port > 65535 || p.Port < 1);
		}
	}
	public static class WebScraperProxyBuilder implements Builder{

		private static final Logger logger = LogManager.getLogger(WebScraperProxyBuilder.class);

		private ArrayList<String> siteToScrapeProxies;
		private Context context;
		private ArrayList<URL> url;

		@Override
		public WebScraperProxyBuilder setProxySite(String... site)
		{
			try
			{
				//this.site = java.net.URLEncoder.encode(site, "UTF-8");
				foreach(String s : site)
				{
					this.siteToScrapeProxies.add(s);
					this.url.add(new URL(s));
				}
			} catch(Exception ex)
			{
				logger.error("Cannot encode url - " + ex.toString());
				System.exit(1);
			}
			return this;
		}

		@Override
		public WebScraperProxyBuilder setContext(Context ctx)
		{
			this.context = ctx;
			return this;
		}

		public WebScraperProxy build()
		{
			WebScraperProxy proxy = new WebScraperProxy(this);
			return proxy;
		}

		public URL getProxyUrl()
		{
			return this.url;
		}
	}

}