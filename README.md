# Description
sql-formatter is a tool used to reformat raw sql written in java string to a more readable way

# Requirement
This project is set to used java 21 as its jdk version. But all the methods or classes are backward compatible. If you need to run to project in jdk 1.8, change the setting in `pom.xml` and set the project sdk to 1.8 in `File`->`Project Structure`->`SDK`

# How to use
1. `Build`->`Build Artifacts`
2. Place a file named `sql.txt` or any other name you like that contains the un-formatted sql under the generated jar file, which should be placed under `out/artifacts/sql_formatter_jar/`
3. Run `java -jar [output.jar] "sql.txt" "output.txt"`, replace sql.txt to the filename you named in step 2. Also, you can name anything you like for the output file
4. The formatted sql should be not in the output file generated under the same directory