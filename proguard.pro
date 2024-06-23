# 打印更加详细的信息，忽略警告
-verbose
-ignorewarnings

# 不要缩减和优化，否则会导致游戏加载失败
-dontshrink
-dontoptimize

# 使用指定名称的混淆
-repackageclasses com.elfmcys.yesstevemodel

# 保留唯一的主模组类和 Mixin 类
-keep class com.elfmcys.yesstevemodel.YesSteveModel
-keep class com.elfmcys.yesstevemodel.mixin.*
-keep class com.elfmcys.yesstevemodel.mixin.client.*
-keep class com.elfmcys.yesstevemodel.mixin.plugin.*

# 保留部分枚举类属性
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留所有用 @Keep 注解标记的方法或者变量
-keepclassmembers class * {
    @com.elfmcys.yesstevemodel.util.Keep <fields>;
    @com.elfmcys.yesstevemodel.util.Keep <methods>;
    @org.spongepowered.asm.mixin.Shadow <fields>;
    @org.spongepowered.asm.mixin.Shadow <methods>;
}

# 保留异常、内部类、注解、行数等信息
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,LineNumberTable,*Annotation*,Synthetic,EnclosingMethod,EventHandler,Override