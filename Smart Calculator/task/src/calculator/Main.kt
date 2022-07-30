package calculator

import java.util.*
import java.util.logging.Logger
import kotlin.NoSuchElementException

class Calculator {
    //private val logger = Logger.getLogger(Calculator::class.java.name)
    val variables = mutableMapOf<String, Int>()

    companion object {
        const val CMD_EXIT = "/exit"
        const val CMD_HELP = "/help"

        val VAR_REGEX = Regex("[a-zA-Z]+")
        val VAR_OR_NUM_REGEX = Regex("[a-zA-Z0-9]+")
    }

    fun start() {
        do {
            val input = readln().trim()
            if (input.isEmpty()) continue
            try {
                when {
                    input == CMD_HELP -> println("The program calculates the operations: + - * / ^ and support brackets")
                    input == CMD_EXIT -> println("Bye!")
                    input.contains('=') -> processVariable(input)
                    else -> processExpression(input)
                }
            } catch (ex: Exception) {
                handleError(input, ex)
            }
        } while (input != CMD_EXIT)
    }

    fun processVariable(input: String) {
        val args = input.split("=").map { it.trim() }.filter { it.isNotEmpty() }
        if (args.size == 1) {
            println(getVariable(args[0]))
        } else if (args.size == 2) {
            if (VAR_REGEX.matches(args[0])) {
                try {
                    variables.put(args[0], getVariable(args[1]))
                } catch (_: NumberFormatException) {
                    println("Invalid assignment")
                }
            } else {
                println("Invalid identifier")
            }
        } else {
            println("Invalid assignment")
        }
    }

    fun processExpression(input: String) {
        val inFixNotation = input
            .replace("(", " ( ")
            .replace(")", " ) ")
            .split(" ").map { it.trim() }.filter { it.isNotEmpty() }
        val postfixNotation = convertToPostfixNotation(inFixNotation)
        val result = executePostfixNotation(postfixNotation)
        println(result)
    }

    fun convertToPostfixNotation(infixNotation: List<String>): List<String> {
        val result = ArrayList<String>()
        val stack = Stack<String>()
        for (item in infixNotation) {
            if (VAR_OR_NUM_REGEX.matches(item)) {
                result.add(item)
            } else if (stack.isEmpty() || "(" == stack.peek() || "(" == item) {
                stack.push(item)
            } else if (")" == item) {
                var s = stack.pop()
                while ("(" != s) {
                    result.add(s)
                    s = stack.pop()
                }
            } else {
                val itemOperator = Operation.parseOperation(item)
                var s = stack.peek()
                while ("(" != s && itemOperator.priority <= Operation.parseOperation(s).priority) {
                    result.add(stack.pop())
                    if (stack.isNotEmpty()) s = stack.peek() else break
                }
                stack.push(item)
            }
        }
        while (stack.isNotEmpty()) result.add(stack.pop())
        //logger.info("RPN: " + result.joinToString(" "))
        return result
    }

    fun executePostfixNotation(postfixNotation: List<String>): Int {
        val stack = Stack<Int>()
        for (item in postfixNotation) {
            if (VAR_OR_NUM_REGEX.matches(item)) {
                stack.push(getVariable(item))
            } else {
                val operation = Operation.parseOperation(item)
                val b = stack.pop()
                val a = stack.pop()
                val operationResult = executeOperation(a, b, operation)
                stack.push(operationResult)
                //logger.info("step: " + a + operation.sign +  b  + " = " + operationResult)
            }
        }
        return stack.pop()
    }

    fun executeOperation(a: Int, b: Int, op: Operation): Int {
        return when (op) {
            Operation.ADD -> a + b
            Operation.SUBSTRACT -> a - b
            Operation.MULTIPLY -> a * b
            Operation.DIVISION -> a / b
            Operation.POWER -> {
                var r = 1
                for (i in 1..b) r *= r
                return r
            }
        }
    }

    fun getVariable(arg: String): Int = if (arg[0].isLetter()) variables.getValue(arg) else arg.toInt()

    fun handleError(input: String, ex: Exception) {
        val msg = when {
            input.startsWith('/') -> "Unknown command"
            ex is NoSuchElementException -> "Unknown variable"
            else -> "Invalid expression"
        }
        println(msg)
    }
}

enum class Operation(val sign: Char, val priority: Int) {
    ADD('+', 0), SUBSTRACT('-', 0),
    MULTIPLY('*', 1), DIVISION('/', 1),
    POWER('^', 2);

    companion object {
        val OP_PLUS_OR_MINUS_REGEX = Regex("\\++|-+")

        fun parseOperation(s: String) : Operation {
            var op : Operation? = null
            if (s.length == 1) {
                val c = s[0]
                for (i in values()) {
                    if (i.sign == c) {
                        op = i
                        break
                    }
                }
            } else if (OP_PLUS_OR_MINUS_REGEX.matches(s)) {
                for (c in s) {
                    if (c == ADD.sign) {
                        op = ADD
                    } else if (c == SUBSTRACT.sign) {
                        op = if (op == SUBSTRACT) ADD else SUBSTRACT
                    }
                }
            }
            if (op == null) {
                throw IllegalArgumentException("Invalid or unknown operation")
            }
            return op
        }
    }
}

fun main() {
    Calculator().start()
}
