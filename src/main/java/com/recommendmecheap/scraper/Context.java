package com.recommendmecheap.scraper;

import java.io.*;

import org.jsoup.*;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import static com.recommendmecheap.scraper.WebScraperProxy.*;

public final class Context {
	public ProxyCache proxyCache = null;
	public DocumentCache documentCache = null;
	public static Context Build()
	{
		Context ctx = new Context();
		return ctx;
	}
	Context InitProxyCache()
	{
		proxyCache = new ProxyCache();
		return this;
	}
	Context InitDocumentCache(String docName)
	{
		documentCache = new DocumentCache(docName);
		return this;
	}
	private Context()
	{

	}
	public static class ProxyCache implements Cache<EProxy> {
		public static final String proxyCacheFile = "/tmp/proxiesKnownToWork.txt"; //TODO make location viable everywhere

		public ProxyCache()
		{

		}
		@Override
		public EProxy Load()
		{
			try {
				BufferedReader reader = new BufferedReader(new FileReader(proxyCacheFile));
				EProxy result = new EProxy();
				String line = "";
				line = reader.readLine();
				String[] data = line.split(",");
				result.Ip = data[0];
				result.Port = Integer.parseInt(data[1]);
				reader.close();
				return result;
			} catch (Exception e) {
				Invalidate();
				return null;
			}
		}

		@Override
		public boolean Invalidate()
		{
			File file = new File(proxyCacheFile);
			return file.delete();
		}

		@Override
		public boolean IsExists()
		{
			File file = new File(proxyCacheFile);
			return file.exists();
		}

		@Override
		public void Write(EProxy t)
		{
			try {
				FileWriter writer = new FileWriter(proxyCacheFile);
				writer.write(t.Ip + "," + t.Port + "\n");
				writer.close();
			} catch (Exception e) {
				//TODO: handle exception
				System.exit(1);
			}
		}
	}

	public static class DocumentCache implements Cache<Document> {
	//For Debug purposes
		public final String proxyCacheFile;

		public DocumentCache(String documentName)
		{
			this.proxyCacheFile = "/tmp/output" + documentName + ".txt";
		}
		@Override
		public Document Load()
		{
			try {
				BufferedReader reader = new BufferedReader(new FileReader(proxyCacheFile));
				reader.close();
				return null;
			} catch (Exception e) {
				System.exit(1);
			}
			return null;
		}

		@Override
		public boolean Invalidate()
		{
			File file = new File(proxyCacheFile);
			return file.delete();
		}

		@Override
		public boolean IsExists()
		{
			File file = new File(proxyCacheFile);
			return file.exists();
		}

		@Override
		public void Write(Document t)
		{
			try {
				FileWriter writer = new FileWriter(proxyCacheFile);
				writer.write(t.toString());
				writer.close();
			} catch (Exception e) {
				//TODO: handle exception
				System.exit(1);
			}
		}
	}
}