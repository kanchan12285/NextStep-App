package com.example.nextstep

// Main scenarios
data class Scenario(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val choices: List<Choice>
)

data class Choice(
    val text: String,
    val outcomeText: String,
    val traits: Map<String, Int>
)

// Friends mode
data class FriendGameRoom(
    val roomCode: String = "",
    val hostId: String = "",
    val hostName: String = "",
    var players: Map<String, FriendPlayerInfo> = emptyMap(),
    val status: String = "waiting",
    val maxPlayers: Int = 4,
    val currentGame: String = "",
    val gameState: Map<String, Any> = emptyMap(),
    val createdAt: Long = 0L
)

data class FriendPlayerInfo(
    val playerId: String = "",
    val playerName: String = "",
    val isHost: Boolean = false,
    val score: Int = 0,
    val isReady: Boolean = false
)

// Online mode
data class OnlineQueueEntry(
    val playerId: String = "",
    val playerName: String = "",
    val timestamp: Long = 0L,
    val status: String = "searching"
)

data class OnlineGameMatch(
    val matchId: String = "",
    val players: Map<String, OnlinePlayerInfo> = emptyMap(),
    val status: String = "starting",
    val currentGame: String = "",
    val gameState: Map<String, Any> = emptyMap(),
    val createdAt: Long = 0L
)

data class OnlinePlayerInfo(
    val playerId: String = "",
    val playerName: String = "",
    val score: Int = 0,
    val isOnline: Boolean = true
)

// Mini game config
data class MiniGameConfig(
    val id: String,
    val name: String,
    val description: String,
    val iconDrawableName: String,
    val activityClass: String,
    val backgroundColor: String
)

// Code Debugger
data class CodeChallenge(
    val id: String,
    val description: String,
    val buggyCode: String,
    val language: String,
    val options: List<String>,
    val correctFix: String,
    val bugExplanation: String,
    val difficulty: String? = null
)

// Color Palette Mixer
data class ColorPaletteChallenge(
    val id: String,
    val description: String,
    val baseColors: List<String>,
    val options: List<List<String>>,
    val correctPalette: List<String>,
    val explanation: String
)

// Empathy Response
data class EmpathyScenario(
    val id: String,
    val message: String,
    val responseOptions: List<String>,
    val correctResponse: String
)

// Fact Checker
data class FactCheckChallenge(
    val id: String,
    val statement: String,
    val isFact: Boolean
)

// Headline Huddle
data class HeadlineChallenge(
    val id: String,
    val articleSummary: String,
    val headlineOptions: List<String>,
    val correctHeadline: String,
    val explanation: String
)

// Lesson Plan Organizer
data class LessonPlanChallenge(
    val id: String,
    val topic: String,
    val objectives: String,
    val activities: List<ActivityItem>,
    val correctOrder: List<String>,
    val explanation: String
)

data class ActivityItem(
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val prerequisites: List<String>
)

// Market Predictor
data class MarketChallenge(
    val scenario: String,
    val marketData: List<String>,
    val options: List<String>,
    val correctPrediction: String,
    val explanation: String
)

// Pattern Finder
data class PatternPuzzle(
    val id: String,
    val sequence: List<String>,
    val question: String,
    val options: List<String>,
    val correctNextElement: String,
    val explanation: String
)

// Resource Allocator
data class AllocationChallenge(
    val scenario: String,
    val availableResources: Map<String, Int>,
    val options: List<AllocationOption>,
    val correctAllocation: String,
    val explanation: String
)

data class AllocationOption(
    val id: String,
    val description: String
)

// Resource Balancer
data class ResourcePuzzle(
    val problemDescription: String,
    val availableResources: Map<String, Int>,
    val tasks: List<Task>,
    val explanation: String
)

data class Task(
    val name: String,
    val priority: String,
    val requiredResources: Map<String, Int>
)

// Slogan Matcher
data class SloganChallenge(
    val id: String,
    val companyName: String,
    val description: String,
    val sloganOptions: List<String>,
    val correctSlogan: String,
    val explanation: String
)

// Symptom Matcher
data class SymptomCase(
    val id: String,
    val patientSymptoms: List<String>,
    val diagnosisOptions: List<String>,
    val correctDiagnosis: String,
    val explanation: String
)

// Threat Identifier
data class ThreatScenario(
    val id: String,
    val description: String,
    val threatOptions: List<String>,
    val correctThreat: String,
    val explanation: String
)

// CARE AI
data class CareerAdvice(
    val summary: String,
    val careers: List<CareerMatch>
)

data class CareerMatch(
    val title: String,
    val insights: String
)

// OpenAI response
data class OpenAIResponse(
    val choices: List<OpenAIChoice>?
)

data class OpenAIChoice(
    val message: OpenAIMessage?
)

data class OpenAIMessage(
    val role: String?,
    val content: String?
)
