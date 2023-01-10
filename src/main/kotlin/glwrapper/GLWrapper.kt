package glwrapper

import kotlin.properties.Delegates

abstract class GLWrapper {
    protected var glRef by Delegates.notNull<Int>()
}