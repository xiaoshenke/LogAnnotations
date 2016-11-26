# LogAnnotations
An annotation for writting log in runtime,may be useful to read other's source code.

## Feature
* Support annotate @LOG to a method 
* Support annotate @NoLog,@LogAll to a class
* Support class heritance:If super class has a method annotated by @LOG,any subclass will produce logs in the method(unless the class is annotated by @NoLog).

### @LOG
You can and only can annotate @LOG to your method.like:                         

```
@LOG
private void call(String name, String xing) {
	;
}
```                       
after build,the origin .java file will be process to like,yes "powed by LOG Annotation" is the produced log identification. 

```
@LOG
private void call(String name, String xing) {
	Log.i("SubA", "in func call ,powed by LOG Annotation");
	;
}
``` 
LOG support 4 level:                               
LEVEL_NO_LOG,	no log will be produced        
LEVEL_INFO,	    produce "Log.i"                           
LEVEL_DEBUG,	produce "Log.d"                        
LEVEL_ERROR,	produce "Log.e"     

### @NoLog
You can and only can annotate @NoLog to your class,like:                       

```
@NoLog
public class SubA extends A
```
Once annotated,any @LOG annotation will be ignored and after build,"ANY LOG PRODUCED BEFORE" will be cleared. You don't have to worry about former produced logs.

### @LogAll
Opposite to @NoLog,@LogAll will annotate every method in the annotated class.LogAll level is defaulted to LEVEL_INFO.like,             
                        
```
@LogAll
public class SubA extends A
```

If the method under the @LogAll annotated class is also annotated in @LOG with different level,we will use @LOG level.   

### TODO
* ~~support @LogAll,@NoLog inheritate attribute~~
* support inner-class
                   
### OTHER
* @LOG,@NoLog,@LogAll don't support interface and anonymous class.

check the code to know more details!







