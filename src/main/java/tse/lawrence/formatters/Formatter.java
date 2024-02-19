package tse.lawrence.formatters;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Formatter
{
	private static final List<String> DISCARD_WORDS = Arrays.asList("\"", "\"+", "+\"", "\"+\"", "+");
	private static final Map<String, String> SQL_SYNTAX_MAP = Stream.of(
			new AbstractMap.SimpleEntry<>("SELECT", "SELECT"),
			new AbstractMap.SimpleEntry<>("FROM", "FROM"),
			new AbstractMap.SimpleEntry<>("LEFT", "LEFT"),
			new AbstractMap.SimpleEntry<>("RIGHT", "RIGHT"),
			new AbstractMap.SimpleEntry<>("JOIN", "JOIN"),
			new AbstractMap.SimpleEntry<>("ON", "ON"),
			new AbstractMap.SimpleEntry<>("WHERE", "WHERE"),
			new AbstractMap.SimpleEntry<>("AND", "AND"),
			new AbstractMap.SimpleEntry<>("OR", "OR"),
			new AbstractMap.SimpleEntry<>("NOT", "NOT"),
			new AbstractMap.SimpleEntry<>("ORDER", "ORDER"),
			new AbstractMap.SimpleEntry<>("BY", "BY"),
			new AbstractMap.SimpleEntry<>("GROUP", "GROUP"),
			new AbstractMap.SimpleEntry<>("LIMIT", "LIMIT"),
			new AbstractMap.SimpleEntry<>("AS", "AS"),
			new AbstractMap.SimpleEntry<>("IN", "IN"),
			new AbstractMap.SimpleEntry<>("BETWEEN", "BETWEEN"),
			new AbstractMap.SimpleEntry<>("ASC", "ASC"),
			new AbstractMap.SimpleEntry<>("DESC", "DESC"),
			new AbstractMap.SimpleEntry<>("UNION", "UNION"),
			new AbstractMap.SimpleEntry<>("ALL", "ALL")
	).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

	private static final List<String> NEWLINE_REQUIRED_SYNTAX = Arrays.asList(
			"SELECT",
			"FROM",
			"LEFT",
			"RIGHT",
			"JOIN",
			"WHERE",
			"ORDER",
			"GROUP",
			"LIMIT",
			"UNION"
	);

	private static final List<String> OPERATORS = Arrays.asList(
			"AND",
			"OR",
			"BETWEEN",
			"ON"
	);

	private static final List<String> FUNCTIONS = Arrays.asList(
			"IFNULL",
			"ROUND",
			"CONCAT",
			"FORMAT",
			"ABS",
			"AVG",
			"CEIL",
			"CEILING",
			"COUNT",
			"FLOOR",
			"MAX",
			"MIN",
			"SIGN",
			"SUM",
			"CURRENT_DATE",
			"DATE_ADD",
			"DATE_FORMAT",
			"CASE",
			"COALESCE",
			"IF",
			"ISNULL"
	);

	public List<List<String>> readFileByLine(String filename) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(filename));

		List<String> lines = reader.lines().collect(Collectors.toList());

		List<List<String>> operatedLines = new ArrayList<>();
		for (String line : lines)
		{
			List<String> wordsInLine = splitBySpace(line);
			List<String> operatedWordsInLine = new ArrayList<>();
			for (String word : wordsInLine)
			{
				word = stripJavaQuotes(word);
				if (containsParentheses(word))
				{
					List<String> splits = Arrays.stream(word.replaceAll("\\(", " ( ")
							.replaceAll("\\)", " ) ")
							.split(" "))
							.map(this::tryToUpperCase)
							.collect(Collectors.toList());

					operatedWordsInLine.addAll(splits);
					continue;
				}
				word = tryToUpperCase(word);
				operatedWordsInLine.add(word);
			}

			operatedLines.add(operatedWordsInLine);
		}

		return operatedLines.stream()
				.map(opLines -> opLines.stream()
						.filter(word -> !word.isEmpty())
						.collect(Collectors.toList())
				).collect(Collectors.toList());
	}

	public String format(List<List<String>> lines)
	{
		List<String> formattedLines = new ArrayList<>();

		List<String> formattedLine = new ArrayList<>();

		int subQueryCount = 0;
		int openParenthesesCount = 0;
		boolean unfinishedBetweenExist = true;
		boolean insideFunction = false;
		String prev = "";

		for (List<String> words : lines)
		{
			for (String word : words)
			{
				if (startOfSubQuery(prev, word))
				{
					subQueryCount += 1;
					flushLine(formattedLines, formattedLine);
					formattedLine.add(indent(subQueryCount) + word);
				}
				else if (isParentheses(word))
				{
					if ("(".equals(word))
					{
						openParenthesesCount += 1;
						formattedLine.add(word);

						if (FUNCTIONS.contains(prev))
						{
							insideFunction = true;
						}
					}
					else if (")".equals(word))
					{
						boolean subQueryFinished = openParenthesesCount == subQueryCount;
						openParenthesesCount -= 1;

						if (insideFunction)
						{
							insideFunction = false;
						}

						if (subQueryFinished)
						{
							subQueryCount -= 1;
							flushLine(formattedLines, formattedLine);
							formattedLine.add(indent(openParenthesesCount) + word);
						}
						else
						{
							formattedLine.add(word);
						}

					}
				}
				else if (NEWLINE_REQUIRED_SYNTAX.contains(word.toUpperCase()))
				{
					if (!isJoinConjunction(prev, word))
					{
						flushLine(formattedLines, formattedLine);
						formattedLine.add(indent(openParenthesesCount) + word);
					}
					else
					{
						formattedLine.add(word);
					}
				}
				else if (OPERATORS.contains(word.toUpperCase()))
				{
					// special handle for AND, BETWEEN
					if (word.equalsIgnoreCase("BETWEEN"))
					{
						unfinishedBetweenExist = true;
						formattedLine.add(word);
					}
					else if (word.equalsIgnoreCase("AND") && unfinishedBetweenExist)
					{
						unfinishedBetweenExist = false;
						formattedLine.add(word);
					}
					else if (")".equals(prev))
					{
						formattedLine.add(word);
					}
					else
					{
						flushLine(formattedLines, formattedLine);
						formattedLine.add(indent(openParenthesesCount + 1) + word);
					}
				}
				else if (word.endsWith(",") && !insideFunction)
				{
					formattedLine.add(word);
					flushLine(formattedLines, formattedLine);
					formattedLine.add(indent(openParenthesesCount + 1));
				}
				else
				{
					formattedLine.add(word);
				}
				prev = word;
			}
		}
		flushLine(formattedLines, formattedLine);

		return String.join("\n", formattedLines);
	}

	private List<String> splitBySpace(String text)
	{
		return Arrays.stream(text.split("\\s+")).collect(Collectors.toList());
	}

	private String stripJavaQuotes(final String word)
	{
		if (DISCARD_WORDS.stream().anyMatch(discard -> discard.equalsIgnoreCase(word)))
		{
			return "";
		}

		String stripped = word;
		boolean stringQuote = word.startsWith("\"") && word.endsWith("\"");
		if (!stringQuote)
		{
			int start = word.startsWith("\"") ? 1 : 0;
			int end = word.endsWith("\"") ? word.length() - 1 : word.length();
			stripped = word.substring(start, end);
		}
		return stripped;
	}

	private String tryToUpperCase(String str)
	{
		return SQL_SYNTAX_MAP.getOrDefault(str.toUpperCase(), str);
	}

	private boolean containsParentheses(String text)
	{
		return text.contains("(") || text.contains(")");
	}

	private boolean isParentheses(String text)
	{
		return "(".equals(text) || ")".equals(text);
	}

	private boolean startOfSubQuery(String prev, String curr)
	{
		return prev.equals("(") && curr.equalsIgnoreCase("SELECT");
	}

	private void flushLine(List<String> lines, List<String> line)
	{
		if (!line.isEmpty())
		{
			String formatted = line.stream().filter(w -> !w.isEmpty()).collect(Collectors.joining(" "));
			lines.add(formatted);
			line.clear();
		}
	}

	private String indent(long repeat)
	{
		StringBuilder indent = new StringBuilder();
		for (int r = 0; r < repeat; r++)
		{
			indent.append('\t');
		}
		return indent.toString();
	}

	private boolean isJoinConjunction(String prev, String word)
	{
		boolean joinConjunction = prev.equalsIgnoreCase("LEFT") || prev.equalsIgnoreCase("RIGHT");
		return word.equalsIgnoreCase("JOIN") && joinConjunction;
	}
}
