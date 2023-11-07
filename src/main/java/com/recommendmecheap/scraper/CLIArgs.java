package com.recommendmecheap.scraper;

public class CLIArgs {
	String proxySite;
	String proxyQuery;
	String eCom;
	String extension;
	String siteToScrape;

	public String toString()
	{
		return proxySite + ";" + proxyQuery + ";" + eCom + ";" + extension + ";" + siteToScrape;
	}
}