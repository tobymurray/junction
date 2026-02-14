package com.technicallyrural.junction.matrix.impl.shortcode

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Service classification result with confidence scoring.
 */
data class ServiceClassification(
    val serviceKey: String,      // e.g., "google_verify", "tangerine_bank", "unknown_83687"
    val serviceName: String,      // e.g., "Google Verification", "Tangerine Banking"
    val confidence: Float,        // 0.0 - 1.0
    val reason: String,           // e.g., "Matched keyword: 'Google Verification Code'"
    val matchedPattern: String? = null  // The actual regex pattern that matched
)

/**
 * Internal classification rule loaded from JSON.
 */
private data class ClassificationRule(
    val serviceKey: String,
    val serviceName: String,
    val description: String,
    val shortCodes: Set<String>,
    val patterns: List<PatternMatcher>
)

/**
 * Pattern matcher with confidence scoring.
 */
private data class PatternMatcher(
    val regex: Regex,
    val confidence: Float,
    val description: String
)

/**
 * Classifies SMS short code messages by service provider using content introspection.
 *
 * Strategy:
 * - Keyword/regex matching against known patterns
 * - Configurable via JSON resource file (res/raw/service_classification_rules.json)
 * - Confidence scoring to avoid false positives
 * - Falls back to per-number mapping if uncertain
 *
 * Thread-safe: Rules loaded once on initialization.
 */
class ServiceClassifier(context: Context) {

    private val rules: List<ClassificationRule>

    companion object {
        private const val TAG = "ServiceClassifier"
        private const val CONFIDENCE_THRESHOLD = 0.7f
        private const val RULES_RESOURCE_NAME = "service_classification_rules"
    }

    init {
        rules = loadRulesFromResource(context)
        Log.d(TAG, "Loaded ${rules.size} classification rules")
    }

    /**
     * Classify a short code message by service.
     *
     * @param shortCode Short code sender (digits only, e.g., "83687")
     * @param messageBody Full SMS message text
     * @param timestamp Message timestamp (for future time-based patterns)
     * @return ServiceClassification with confidence score
     */
    fun classifyMessage(
        shortCode: String,
        messageBody: String,
        timestamp: Long = System.currentTimeMillis()
    ): ServiceClassification {
        Log.d(TAG, "Classifying message from $shortCode (body length: ${messageBody.length})")

        // 1. Try short code whitelist first (highest confidence)
        for (rule in rules) {
            if (shortCode in rule.shortCodes) {
                Log.d(TAG, "Short code $shortCode whitelisted for service ${rule.serviceKey}")
                // Still verify with at least one pattern to avoid misclassification
                val patternMatch = findBestPatternMatch(rule, messageBody)
                if (patternMatch != null && patternMatch.confidence >= CONFIDENCE_THRESHOLD) {
                    return ServiceClassification(
                        serviceKey = rule.serviceKey,
                        serviceName = rule.serviceName,
                        confidence = patternMatch.confidence,
                        reason = "Short code ${shortCode} whitelisted + pattern match: ${patternMatch.description}",
                        matchedPattern = patternMatch.regex.pattern
                    )
                }
            }
        }

        // 2. Try pattern matching across all rules
        var bestMatch: Pair<ClassificationRule, PatternMatcher>? = null
        var bestConfidence = 0f

        for (rule in rules) {
            val patternMatch = findBestPatternMatch(rule, messageBody)
            if (patternMatch != null && patternMatch.confidence > bestConfidence) {
                bestConfidence = patternMatch.confidence
                bestMatch = rule to patternMatch
            }
        }

        // 3. Return best match if above threshold
        if (bestMatch != null && bestConfidence >= CONFIDENCE_THRESHOLD) {
            val (rule, pattern) = bestMatch
            Log.d(TAG, "Classified $shortCode as ${rule.serviceKey} (confidence=$bestConfidence)")
            return ServiceClassification(
                serviceKey = rule.serviceKey,
                serviceName = rule.serviceName,
                confidence = bestConfidence,
                reason = "Pattern match: ${pattern.description}",
                matchedPattern = pattern.regex.pattern
            )
        }

        // 4. Fallback: treat as unique sender
        Log.d(TAG, "No confident match for $shortCode (best confidence=$bestConfidence), using per-number mapping")
        return ServiceClassification(
            serviceKey = "unknown_$shortCode",
            serviceName = "SMS Short Code: $shortCode",
            confidence = 1.0f,
            reason = "No matching service pattern (fallback to per-number mapping)",
            matchedPattern = null
        )
    }

    /**
     * Find the best matching pattern for a rule.
     */
    private fun findBestPatternMatch(
        rule: ClassificationRule,
        messageBody: String
    ): PatternMatcher? {
        var bestMatch: PatternMatcher? = null
        var bestConfidence = 0f

        for (pattern in rule.patterns) {
            try {
                if (pattern.regex.containsMatchIn(messageBody)) {
                    if (pattern.confidence > bestConfidence) {
                        bestConfidence = pattern.confidence
                        bestMatch = pattern
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Regex match error for pattern ${pattern.regex.pattern}", e)
            }
        }

        return bestMatch
    }

    /**
     * Load classification rules from JSON resource file.
     */
    private fun loadRulesFromResource(context: Context): List<ClassificationRule> {
        try {
            val resourceId = context.resources.getIdentifier(
                RULES_RESOURCE_NAME,
                "raw",
                context.packageName
            )

            if (resourceId == 0) {
                Log.e(TAG, "Classification rules resource not found: $RULES_RESOURCE_NAME")
                return emptyList()
            }

            val inputStream = context.resources.openRawResource(resourceId)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }

            val json = JSONObject(jsonString)
            val rulesArray = json.getJSONArray("rules")

            return (0 until rulesArray.length()).map { i ->
                parseRule(rulesArray.getJSONObject(i))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load classification rules", e)
            return emptyList()
        }
    }

    /**
     * Parse a single classification rule from JSON.
     */
    private fun parseRule(ruleJson: JSONObject): ClassificationRule {
        val serviceKey = ruleJson.getString("serviceKey")
        val serviceName = ruleJson.getString("serviceName")
        val description = ruleJson.optString("description", "")

        val shortCodesArray = ruleJson.getJSONArray("shortCodes")
        val shortCodes = (0 until shortCodesArray.length())
            .map { shortCodesArray.getString(it) }
            .toSet()

        val patternsArray = ruleJson.getJSONArray("patterns")
        val patterns = (0 until patternsArray.length()).map { i ->
            parsePattern(patternsArray.getJSONObject(i))
        }

        return ClassificationRule(
            serviceKey = serviceKey,
            serviceName = serviceName,
            description = description,
            shortCodes = shortCodes,
            patterns = patterns
        )
    }

    /**
     * Parse a single pattern matcher from JSON.
     */
    private fun parsePattern(patternJson: JSONObject): PatternMatcher {
        val regexString = patternJson.getString("regex")
        val confidence = patternJson.getDouble("confidence").toFloat()
        val description = patternJson.optString("description", "")

        return PatternMatcher(
            regex = Regex(regexString),
            confidence = confidence,
            description = description
        )
    }
}
