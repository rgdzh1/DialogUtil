### DialogUtil
[源码解析文章](https://blog.csdn.net/MoLiao2046/article/details/105795020)
#### 使用
**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```
	allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
```
**Step 2.** Add the dependency
```
	dependencies {
	        compile ('com.github.hss01248:DialogUtil:lastest release'){
              exclude group: 'com.android.support'
	        }
	         compile 'com.android.support:appcompat-v7:26.1.0'
   			 compile 'com.android.support:recyclerview-v7:26.1.0'
    		 compile 'com.android.support:design:26.1.0'
    		 //将26.1.0: 改为自己项目中一致的版本
	}
```
##### 初始化
```
//在Application的oncreate方法里:
传入context
StyledDialog.init(this);
在activity生命周期callback中拿到顶层activity引用:
 registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                ActivityStackManager.getInstance().addActivity(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                ActivityStackManager.getInstance().removeActivity(activity);
            }
        });

```
#### 示例代码(MainActivity里)
```
//使用默认样式时,无须.setxxx:
StyledDialog.buildLoading().show();

//自定义部分样式时:
StyledDialog.buildMdAlert("title", msg,  new MyDialogListener() {
            @Override
            public void onFirst() {
                showToast("onFirst");
            }

            @Override
            public void onSecond() {
                showToast("onSecond");
            }

            @Override
            public void onThird() {
                showToast("onThird");
            }


        })
                .setBtnSize(20)
                .setBtnText("i","b","3")
                .show();
```

#### 相关回调
##### MyDialogListener
```
	public abstract void onFirst();//md-确定,ios-第一个
    public abstract void onSecond();//md-取消,ios-第二个
    public void onThird(){}//md-netural,ios-第三个
    public void onCancle(){}
    /**
     * 提供给Input的回调
     * @param input1
     * @param input2
     */
    public void onGetInput(CharSequence input1,CharSequence input2){

    }

    /**
     * 提供给MdSingleChoose的回调
     * @param chosen
     * @param chosenTxt
     */
    public void onGetChoose(int chosen,CharSequence chosenTxt){

    }

    /**
     * 提供给MdMultiChoose的回调
     * @param states
     */
    public void onChoosen( List<Integer> selectedIndex, List<CharSequence> selectedStrs,boolean[] states){

    }
```
##### MyItemDialogListener
```
 /**
     * IosSingleChoose,BottomItemDialog的点击条目回调
     * @param text
     * @param position
     */
   public abstract void onItemClick(CharSequence text, int position);


    /**
     * BottomItemDialog的底部按钮(经常是取消)的点击回调
     */
   public void onBottomBtnClick(){}
```
#### 提供的api
##### 对话框的消失
```
StyledDialog.dismiss(DialogInterface... dialogs);
```
##### 两个loading对话框不需要对象就可以直接dismisss:
```
StyledDialog.dismissLoading();
```
##### progress dialog 的进度更新
```
/**
 *  可以在任何线程调用
 * @param dialog 传入show方法返回的对象
 * @param progress
 * @param max
 * @param msg 如果是转圈圈,会将msg变成msg:78%的形式.如果是水平,msg不起作用
 * @param isHorizontal 是水平线状,还是转圈圈
 */
public static void updateProgress( Dialog dialog, int progress,  int max,  CharSequence msg,  boolean isHorizontal)
```

