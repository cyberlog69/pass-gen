package com.cyberlog69.passgen.domain

object LeakedPasswordChecker {

    private val COMMON_LEAKED_PASSWORDS: Set<String> = hashSetOf(
        "password", "123456", "123456789", "guest", "qwerty", "12345678", "111111",
        "12345", "1234567", "dragon", "p@ssword", "welcome", "123123", "football",
        "baseball", "monkey", "letmein", "princess", "sunshine", "iloveyou", "trustno1",
        "admin", "admin123", "pass123", "welcome1", "superman", "master", "shadow",
        "charlie", "starwars", "killer", "666666", "777777", "888888", "999999",
        "000000", "hello", "secret", "default", "root", "toor", "test", "login",
        "hunter2", "myspace1", "matrix", "super123", "qwertyuiop", "asdfghjkl",
        "zxcvbnm", "password1", "passphrase", "abc123", "1q2w3e4r", "0123456789",
        "trustme", "welcome@123", "system", "pass@123", "server", "oracle", "database",
        "access", "changeme", "manager", "security", "secure123", "cisco", "cisco123",
        "spring2024", "summer2024", "winter2024", "fall2024", "coffee", "computer",
        "internet", "freedom", "forever", "football1", "arsenal", "chelsea", "liverpool",
        "barcelona", "realmadrid", "pokemon", "naruto", "dragonball", "batman", "spiderman",
        "ironman", "avengers", "disney", "starwars1", "jordan", "kobe24", "michael",
        "jessica", "daniel", "ashley", "matthew", "amanda", "anthony", "jennifer",
        "joshua", "brittany", "david", "samantha", "andrew", "taylor", "james",
        "elizabeth", "justin", "stephanie", "joseph", "nicole", "william", "megan",
        "robert", "alexandra", "christopher", "hannah", "ryan", "rachel", "nicholas",
        "sarah", "brandon", "kayla", "victoria", "tyler", "emily", "alexander",
        "rebecca", "samuel", "lauren", "john", "morgan", "dylan", "melissa",
        "christian", "michelle", "benjamin", "amber", "zachary", "courtney"
    )

    data class LeakResult(
        val isLeaked: Boolean,
        val reason: String? = null
    )

    fun check(password: String?): LeakResult {
        if (password.isNullOrBlank()) {
            return LeakResult(isLeaked = false)
        }

        val clean = password.trim().lowercase()

        // 1. Direct dictionary match
        if (COMMON_LEAKED_PASSWORDS.contains(clean)) {
            return LeakResult(
                isLeaked = true,
                reason = "Known breached password found in public leak digests."
            )
        }

        // 2. Pure single-character repetition (e.g., 'aaaaaa', '1111111')
        if (password.length >= 4 && password.all { it == password[0] }) {
            return LeakResult(
                isLeaked = true,
                reason = "Trivial repeated character pattern."
            )
        }

        // 3. Trivial sequential digits
        if (clean.matches(Regex("^(0123456789|123456789|9876543210|987654321)+$"))) {
            return LeakResult(
                isLeaked = true,
                reason = "Trivial numeric keyboard sequence."
            )
        }

        // 4. Keyboard walk patterns
        val walks = listOf("qwerty", "asdfgh", "zxcvbn", "1qaz2wsx")
        for (walk in walks) {
            if (clean.contains(walk)) {
                return LeakResult(
                    isLeaked = true,
                    reason = "Common keyboard walk pattern detected."
                )
            }
        }

        return LeakResult(isLeaked = false)
    }
}
