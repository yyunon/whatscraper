package com.recommendmecheap.scraper;

import java.util.*;

import org.apache.commons.cli.*;

public final class CLI {
	public CLIArgs cliArgs;
	private Options options;
	private CommandLineParser parser;
	private HelpFormatter formatter;
	private CommandLine cmd;
	private String[] args;

	public static CLI Build(String[] args)
	{
		CLI cli = new CLI(args);
		return cli;
	}
	private CLI(String[] args)
	{
		this.args = args;
		this.options = new Options();
		this.parser = new DefaultParser();
		this.formatter = new HelpFormatter();
		this.cliArgs = new CLIArgs();
		this.cmd = null;

		Option verbosity = new Option("v", "verbosity" ,true, "Log level");
		Option site = Option.builder("s").longOpt("site").desc("Site to scrape proxy websites").hasArg().build();
		Option query = Option.builder("q").longOpt("query").desc("Site to scrape proxy websites").hasArg().build();
		Option eCommerce = Option.builder("e").longOpt("ecommerce").desc("Site to scrape").hasArg().build();
		Option locale = Option.builder("l").longOpt("locale").desc("Locale").hasArg().build();

		AddOptions(site, verbosity, query, eCommerce, locale);
	}

	public void Parse()
	{
		try
		{
				cmd = parser.parse(options, args);

				cliArgs.proxySite = cmd.getOptionValue("s");

				cliArgs.proxyQuery = cmd.getOptionValue("q");

				cliArgs.eCom = cmd.getOptionValue("e");

				cliArgs.extension = cmd.getOptionValue("l");

				cliArgs.siteToScrape = ECommerce.WebSite(cliArgs.eCom, cliArgs.extension);
		} catch (ParseException ex)  {
				System.out.println("Exception in cli - " + ex.toString());
				formatter.printHelp("scraper",options,true);
				System.exit(1);
		}
	}

	private void AddOptions(Option ... o)
	{
			for(Option i : o)
			{
					this.options.addOption(i);
			}
	}
	
}