### 安装darcula到本地仓库命令示例 ###
```shell
mvn install:install-file -Dfile=E:\IdeaWorkspace\fangxuele-tool-wechat-push\src\main\lib\darcula.jar -DgroupId=com.darcula -DartifactId=darcula-lnf -Dpackaging=jar -Dversion=1.0 -DgeneratePom=true -DcreateChecksum=true
```
### 安装beauty-eye到本地仓库命令示例 ###
```shell
mvn install:install-file -Dfile=E:\IdeaWorkspace\fangxuele-tool-wechat-push\src\main\lib\beautyeye_lnf.jar -DgroupId=com.beautyeye -DartifactId=beautyeye-lnf -Dpackaging=jar -Dversion=1.0 -DgeneratePom=true -DcreateChecksum=true
```

### 关于多线程并发下的原子操作
> volatile解决多线程内存不可见问题。对于一写多读，是可以解决变量同步问题， 
但是如果多写，同样无法解决线程安全问题。
>
> 如果是 count++操作，使用如下类实现： 
AtomicInteger count = new AtomicInteger(); 
count.addAndGet(1); 
如果是 JDK8，推荐使用 LongAdder对象，比 AtomicLong性能更好（减少乐观锁的重试次数）