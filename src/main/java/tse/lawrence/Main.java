package tse.lawrence;

import tse.lawrence.formatters.Formatter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


public class Main
{
	public static void main(String[] args)
	{
		String fileRead = "./sql.txt";
		String fileWrite = "./formatted.txt";

		if (args.length >= 1)
		{
			fileRead = args[0];
			if (args.length >= 2)
			{
				fileWrite = args[1];
			}
		}

		run(fileRead, fileWrite);
	}

	private static void run(String fileRead, String fileWrite)
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileWrite)))
		{
			System.out.println("Formatting...");
			Formatter formatter = new Formatter();
			List<List<String>> lines = formatter.readFileByLine(fileRead);
			String formatted = formatter.format(lines);
			writer.write(formatted);
			System.out.println(formatted);
			System.out.printf("Formatted sql is placed in %s", fileWrite);
		}
		catch (IOException e)
		{
			System.out.println(e.getMessage());
		}
	}
}
