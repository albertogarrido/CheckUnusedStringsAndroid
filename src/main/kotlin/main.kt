import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

fun main(vararg args: String) {
    val result = checkParams(args)
    val (pathToProject, pathToStrings) = when (result) {
        is Result.Err -> {
            System.err.println("${ANSI_RED}${result.throwable!!.message}${ANSI_RESET}")
            return
        }

        is Result.Ok -> {
            result.data!!
        }
    }
    checkForUnusedStrings(pathToProject, pathToStrings)
}

fun checkForUnusedStrings(
    pathToProject: String,
    pathToStrings: String
) {
    println("$ANSI_BLUE- finding all the strings in $ANSI_RESET")
    println("$ANSI_BLUE      -> $pathToStrings$ANSI_RESET")
    val list = generateList(readFile(Paths.get(pathToStrings)))
    println("$ANSI_GREEN- found ${list.size} strings$ANSI_RESET")
    val notUsedStrings = mutableListOf<String>()
    println("$ANSI_BLUE- searching for unused strings...$ANSI_RESET")
    list.forEachIndexed { index, it ->
        if (!findUsage(
                path = pathToProject,
                string = it.name,
                stringNum = index + 1,
                totalStrings = list.size
            )
        ) {
            notUsedStrings.add(it.name)
        }
    }
    notUsedStrings.forEach { println(it) }
    if (notUsedStrings.isEmpty()) println("$ANSI_GREEN- no unused strings found!$ANSI_RESET")
    else println("${ANSI_YELLOW}Not used strings: ${notUsedStrings.size}$ANSI_RESET")
}

fun checkParams(args: Array<out String>): Result<Pair<String, String>> {
    if (args.size > 1) {
        return Result.Err(IllegalArgumentException("Error: too many arguments. Please add the path to your android project"))
    }

    if (args.isEmpty()) {
        return Result.Err(IllegalArgumentException("Error: too many arguments. Please add the path to your android project"))
    }

    val pathArg = args.first()

    val rootPath = if (pathArg.endsWith("/")) {
        pathArg
    } else {
        "${pathArg}/"
    }

    val pathToStrings = Paths.get(rootPath + STRINGS_EN)
    if (!Files.exists(pathToStrings)) {
        return Result.Err(IllegalArgumentException("Error: $rootPath doesn't seem to be a valid android project."))
    }
    return Result.Ok(rootPath to pathToStrings.toString())
}

fun findUsage(
    path: String,
    string: String,
    stringNum: Int,
    totalStrings: Int
): Boolean {
    print("   ${(stringNum * 100) / totalStrings}%  \r")
    File(path).walk(FileWalkDirection.BOTTOM_UP).forEach { file ->
        if (isExcluded(file)) return@forEach
        if (file.name.endsWith(JAVA_FILES)
            || file.name.endsWith(KOTLIN_FILES)
            || file.name.endsWith(XML_FILES)
            && !file.name.equals("strings.xml")
            && !file.name.equals("merger.xml")
        ) {
            val contents = Files.readString(file.toPath())
            if (contents.contains(string)) {
                return true
            }
        }
    }
    return false
}

fun isExcluded(file: File) =
    file.path.contains("/build/") ||
            file.name.startsWith(".") ||
            file.name.endsWith(".mg", ignoreCase = true)

fun readFile(path: Path) = readXml(File(path.toString()))

fun readXml(file: File): Document {
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(file.readText()))
    return dBuilder.parse(xmlInput)
}

fun generateList(document: Document): List<StringValues> {
    val retValue = mutableListOf<StringValues>()
    val strings = document.getElementsByTagName("string")
    for (i in 0 until strings.length) {
        val item = strings.item(i)
        val stringName = item.attributes.getNamedItem("name").nodeValue
        val translatable = item.attributes.getNamedItem("translatable")?.nodeValue ?: "true"
        val stringValue = item.textContent
        retValue.add(
            StringValues(
                name = stringName,
                value = stringValue,
                translatable = translatable != "false"
            )
        )
    }
    return retValue
}

data class StringValues(
    val name: String,
    val value: String,
    val translatable: Boolean = true
)

sealed class Result<T>(val data: T? = null, val throwable: Throwable? = null) {
    class Ok<T>(data: T) : Result<T>(data)
    class Err<T>(throwable: Throwable?, data: T? = null) : Result<T>(data, throwable)
}

const val STRINGS_EN = "app/src/main/res/values/strings.xml"

const val JAVA_FILES = ".java"
const val KOTLIN_FILES = ".kt"
const val XML_FILES = ".xml"

const val ANSI_RESET = "\u001B[0m"
const val ANSI_BLACK = "\u001B[30m"
const val ANSI_RED = "\u001B[31m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_YELLOW = "\u001B[33m"
const val ANSI_BLUE = "\u001B[34m"
const val ANSI_PURPLE = "\u001B[35m"
const val ANSI_CYAN = "\u001B[36m"
const val ANSI_WHITE = "\u001B[37m"
