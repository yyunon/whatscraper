package com.recommendmecheap.scraper;

public final class ECommerce {
	public static final String Amazon = "https://www.amazon.@COM/s?k=";
	public static final String Ebay = "https://www.ebay.@COM/sch/i.html?_nkw=";
	public static final String Alibaba = "https://www.alibaba.@COM//trade/search?SearchText=";

	public static String WebSite(String in, String code)
	{
		String parsedCode = ECommerce.CountryCode.Parse(code);
		if(in.toLowerCase().equals("amazon"))
		{
			return ECommerce.Amazon.replace("@COM",parsedCode );
		} else if(in.toLowerCase().equals("ebay"))
		{
			return ECommerce.Ebay.replace("@COM", parsedCode);
		} else if(in.toLowerCase().equals("alibaba"))
		{
			return ECommerce.Alibaba.replace("@COM", parsedCode);
		} 
		return "";
	}
	public static class CountryCode 
	{
		public static final String NL = "nl";
		public static final String DE = "de";
		public static final String COM = "com";

		public static String Parse(String in)
		{
			if(in.toLowerCase().equals("nl"))
			{
				return NL;
			} else if(in.toLowerCase().equals("de"))
			{
				return DE;
			} else if(in.toLowerCase().equals("com"))
			{
				return COM;
			} 
			return "";
		}

	}
}