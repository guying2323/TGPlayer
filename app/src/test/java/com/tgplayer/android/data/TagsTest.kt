package com.tgplayer.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TagsTest {

    @Test
    fun extractsLowercaseUniqueTags() {
        assertEquals(
            listOf("猫", "cute_cat", "daily"),
            MediaMapper.tagsOf("两只 #猫 一只 #Cute_Cat #猫 #daily"),
        )
    }

    @Test
    fun ignoresHashWithoutWord() {
        assertEquals(emptyList<String>(), MediaMapper.tagsOf("# #! ###"))
        assertEquals(emptyList<String>(), MediaMapper.tagsOf(null))
    }

    @Test
    fun capsAtEightTags() {
        val text = (1..12).joinToString(" ") { "#tag$it" }
        assertEquals(8, MediaMapper.tagsOf(text).size)
    }
}
