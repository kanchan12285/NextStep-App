package com.example.nextstep

import android.content.Context
import org.json.JSONArray
import android.util.Log
import java.io.InputStream

// Choice and Scenario data classes are imported from DataModels.kt.

class GameDataManager(private val context: Context) {

    private val TAG = "GameDataManager"
    private val scenarios = mutableListOf<Scenario>() // This is your parsed scenarios list
    private val servedScenarioIds = mutableSetOf<String>() // To track already-served scenarios

    init {
        loadScenariosFromJson()
    }

    fun getRoundScenarios(): List<Scenario> {
        // Filter scenarios not served yet
        val remainingScenarios = scenarios.filter { it.id !in servedScenarioIds }

        val selectedScenarios = if (remainingScenarios.size <= 10) {
            remainingScenarios.shuffled()
        } else {
            remainingScenarios.shuffled().take(10)
        }

        servedScenarioIds.addAll(selectedScenarios.map { it.id })

        // Reset when all have been served to allow repetition
        if (servedScenarioIds.size == scenarios.size) {
            servedScenarioIds.clear()
        }

        return selectedScenarios
    }

    // Loads scenarios from the 'game_scenarios.json' file in the assets folder
    private fun loadScenariosFromJson() {
        try {
            val inputStream: InputStream = context.assets.open("game_scenarios.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val scenarioJson = jsonArray.getJSONObject(i)
                val choicesJsonArray = scenarioJson.getJSONArray("choices")
                val choices = mutableListOf<Choice>()

                for (j in 0 until choicesJsonArray.length()) {
                    val choiceJson = choicesJsonArray.getJSONObject(j)
                    val traitsJson = choiceJson.getJSONObject("traits")
                    val traits = mutableMapOf<String, Int>()

                    traitsJson.keys().forEach { key ->
                        traits[key] = traitsJson.getInt(key)
                    }

                    choices.add(
                        Choice(
                            text = choiceJson.getString("text"),
                            traits = traits,
                            outcomeText = choiceJson.getString("outcomeText")
                        )
                    )
                }

                scenarios.add(
                    Scenario(
                        id = scenarioJson.getString("id"),
                        title = scenarioJson.getString("title"),
                        description = scenarioJson.getString("description"),
                        imageUrl = scenarioJson.getString("imageUrl"),
                        choices = choices
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading scenarios from JSON: ${e.message}", e)
        }
    }
}
