package ru.okcode.icalc.core

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.okcode.icalc.command.Operand
import ru.okcode.icalc.command.Operator
import ru.okcode.icalc.command.operator.Percent
import ru.okcode.icalc.utils.ZERO
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

class CalcProcessorImpl @Inject constructor(
    private val textProcessor: TextProcessor
) : CalcProcessor {


    private var numbersStack = Stack<BigDecimal>()
    private var backupNumberStack = Stack<BigDecimal>()
    private val operatorsStack = Stack<Operator>()

    private var nextOperator: Operator? = null

    private val _displayResult = MutableLiveData<String>()
    override val displayResult: LiveData<String>
        get() = _displayResult

    private var lastInputIsOperator = false

    init {
        _displayResult.value = textProcessor.nextNumberAsText
    }

    /**
     * Handle Operand
     */
    override fun handleOperand(operand: Operand) {
        textProcessor.createNextNumberAsText(operand)
        _displayResult.value = textProcessor.nextNumberAsText
        lastInputIsOperator = false
    }

    /**
     * Handle Operator
     */
    override fun handleOperator(operator: Operator) {

        if (operator is Percent) {
            handlePercent()
            _displayResult.value = textProcessor.nextNumberAsText
            return
        }

        nextOperator = operator
        handleNextNumber(displayResult.value)

        if (lastInputIsOperator) {
            rollback()
        }

        if (operatorsStack.isEmpty()) {
            operatorsStack.push(operator)
        } else {
            val lastOperatorInStack = operatorsStack.peek()
            if (operator.rang() <= lastOperatorInStack.rang()) {
                calculateAllOperatorsFromStack()
                operatorsStack.push(operator)
            }
        }
        lastInputIsOperator = true
    }

    private fun handlePercent() {
        val nextNumber = textProcessor.convertToNumber(textProcessor.nextNumberAsText)
        val percent = nextNumber.div(BigDecimal(100))
        textProcessor.nextNumberAsText = textProcessor.convertToText(percent)
    }

    private fun rollback() {
        numbersStack = backupNumberStack
        if (operatorsStack.isNotEmpty()) {
            operatorsStack.pop()
        }
    }

    private fun calculateAllOperatorsFromStack() {
        backupNumberStack = numbersStack
        while (operatorsStack.isNotEmpty()) {
            val numberB: BigDecimal
            val numberA: BigDecimal

            when {
                numbersStack.isEmpty() -> {
                    numberB = BigDecimal.valueOf(0.0)
                    numberA = BigDecimal.valueOf(0.0)
                }
                numbersStack.size == 1 -> {
                    numberB = numbersStack.pop()
                    numberA = BigDecimal.valueOf(0.0)
                }
                else -> {
                    numberB = numbersStack.pop()
                    numberA = numbersStack.pop()
                }
            }

            val operator = operatorsStack.pop()
            try {
                val calcResult = operator.calc(numberA, numberB)
                _displayResult.value = textProcessor.convertToText(calcResult)
                numbersStack.push(calcResult)
            } catch (exception: ArithmeticException) {
                _displayResult.value = exception.message
                numbersStack.clear()
                operatorsStack.clear()
                nextOperator = null
            }
        }
    }

    private fun handleNextNumber(nextNumberAsText: String?) {
        val nextNumber: BigDecimal = if (nextNumberAsText == null) {
            BigDecimal.valueOf(0.0)
        } else {
            convertToNumber(nextNumberAsText)
        }

        numbersStack.push(nextNumber)
        textProcessor.nextNumberAsText = ZERO
    }

    private fun convertToNumber(numberAsText: String): BigDecimal {
        return textProcessor.convertToNumber(numberAsText)
    }

    /**
     * Handle Clear
     */
    override fun handleClear(clearAll: Boolean) {
        if (clearAll) {
            numbersStack.clear()
            operatorsStack.clear()
            nextOperator = null
        }
        textProcessor.nextNumberAsText = ZERO
        _displayResult.value = textProcessor.nextNumberAsText
    }

    /**
     * Handle Equally
     */
    override fun handleEqually() {
        if (nextOperator != null) {
            handleOperator(nextOperator!!)
        } else {
            calculateAllOperatorsFromStack()
        }

        numbersStack.clear()
        operatorsStack.clear()

        nextOperator = null
    }

}