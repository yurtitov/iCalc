package ru.okcode.icalc.command.operand

import ru.okcode.icalc.command.Command

interface Operand: Command {

    fun getResultNumber(oldNumber: Double): Double

}