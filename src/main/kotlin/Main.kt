package com.melancholicbastard

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import com.melancholicbastard.config.ConfigLoader


fun main(args: Array<String>) {
    val configArg = args.find { it.startsWith("--config=") }?.substringAfter("=")
    if (configArg == null) {
        System.err.println("Укажите путь к конфигурации флагом --config=path/to/config.csv")
        kotlin.system.exitProcess(1)
    }

    when (val res = ConfigLoader.load(configArg)) {
        is ConfigLoader.Result.Error -> {
            System.err.println("Ошибки конфигурации:")
            res.errors.forEach { System.err.println(" - $it") }
            kotlin.system.exitProcess(1)
        }

        is ConfigLoader.Result.Ok -> {
            // Этап 3: вывод всех параметров ключ-значение
            res.config.asKeyValuePairs().forEach { (k, v) ->
                println("$k=$v")
            }
            // Здесь далее можно будет вызвать построение графа, используя res.config.maxDepth и т.п.
        }
    }
}


