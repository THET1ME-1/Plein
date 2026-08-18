package app.plein.folders

import app.plein.data.FolderRules
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Живая папка: собирается по правилу и следит за собой сама.
 *
 * Поставили новую игру — она легла в «Игры» без спроса. Удалили — пропала.
 * А вот если человек вынул приложение руками, обратно оно не возвращается:
 * ручная правка сильнее правила, иначе лаунчер спорит с хозяином.
 */
class FolderRulesTest {

    private val games = listOf("шахматы", "гонки", "тетрис")

    @Test
    fun `новое приложение категории ложится само`() {
        val kept = FolderRules.apply(current = listOf("шахматы", "гонки"), matching = games, removed = emptySet())
        assertEquals(listOf("шахматы", "гонки", "тетрис"), kept)
    }

    @Test
    fun `удалённое приложение уходит из папки`() {
        val kept = FolderRules.apply(current = listOf("шахматы", "гонки"), matching = listOf("шахматы"), removed = emptySet())
        assertEquals(listOf("шахматы"), kept)
    }

    @Test
    fun `вынутое руками не возвращается`() {
        val kept = FolderRules.apply(current = listOf("шахматы"), matching = games, removed = setOf("гонки"))
        assertEquals(listOf("шахматы", "тетрис"), kept)
    }

    @Test
    fun `порядок, выставленный руками, не сбивается`() {
        // Человек перетащил тетрис наверх — новые приложения идут в конец,
        // а его расстановку трогать нельзя.
        val kept = FolderRules.apply(
            current = listOf("тетрис", "шахматы"),
            matching = games + "судоку",
            removed = emptySet(),
        )
        assertEquals(listOf("тетрис", "шахматы", "гонки", "судоку"), kept)
    }

    @Test
    fun `чужое приложение, добавленное руками, остаётся`() {
        // Правило собирает игры, но человек положил сюда же калькулятор.
        val kept = FolderRules.apply(current = listOf("шахматы", "калькулятор"), matching = games, removed = emptySet(), keepStrangers = true)
        assertEquals(listOf("шахматы", "калькулятор", "гонки", "тетрис"), kept)
    }
}
