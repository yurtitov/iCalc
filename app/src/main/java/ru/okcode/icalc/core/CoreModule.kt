package ru.okcode.icalc.core

import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
abstract class CoreModule {

    @Binds
    @Singleton
    abstract fun bindCalcProcessor(processor: CalcProcessorImpl): CalcProcessor

    @Binds
    @Singleton
    abstract fun bindTextProcessor(processor: TextProcessorImpl): TextProcessor
}