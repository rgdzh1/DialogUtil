package com.hss01248.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;

import com.hss01248.dialog.adapter.SuperLvHolder;
import com.hss01248.dialog.config.ConfigBean;
import com.hss01248.dialog.config.DefaultConfig;
import com.hss01248.dialog.ios.IosAlertDialogHolder;
import com.hss01248.dialog.material.MdInputHolder;
import com.hss01248.dialog.view.DialogUtil_DialogActivity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/10/9 0009.
 */
public class Tool {

    public static void dismiss(ConfigBean bean, boolean isAfterResult) {

        if (isAfterResult && !bean.dismissAfterResultCallback) {
            return;
        }

        //先隐藏keyboard 隐藏键盘先
        hideKeyBorad(bean);
        if (bean.showAsActivity) {
            Activity activity = ActivityStackManager.getInstance().getTopActivity(DialogUtil_DialogActivity.class);
            if (activity != null) {
                activity.finish();
            }
            return;
        }
        if (bean.showAsFragment) {
            if (bean.mDialogFragment != null) {
                bean.mDialogFragment.dismiss();
                return;
            }
        }
        if (bean.dialog != null) {
            bean.dialog.dismiss();
        }
        if (bean.alertDialog != null) {
            bean.alertDialog.dismiss();
        }
    }

    public static void dismiss(ConfigBean bean) {
        dismiss(bean, false);
    }


    public static Handler getMainHandler() {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        return mainHandler;
    }

    private static Handler mainHandler;

    /**
     * 解决badtoken问题,一劳永逸
     *
     * @param dialog
     */
    public static void showDialog(final Dialog dialog, final ConfigBean bean) {
        // StyledDialog.getMainHandler() 这个Handler在初始化的时候是在主线程中初始化的, 那么通过这个Handler可以将消息发送到主线程UI去操作
        // 这里是直接post了一个Runnable对象, 最终就会执行run(),run方法中主要是显示出Dialog用的.
        StyledDialog.getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    // dialog 只有show出来之后菜可以调整窗体大小.
                    dialog.show();
                    // 调整窗体大小
                    adjustWindow(dialog, bean);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public static void setListener(final Dialog dialog, final ConfigBean bean) {
        if (dialog == null) {
            return;
        }

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog0) {
                if (bean.alertDialog != null) {
                    setMdBtnStytle(bean);
                    setTitleMessageStyle(bean.alertDialog, bean);
                }
                bean.listener.onShow();
                //将activity与dialog保管起来用map与set集合.
                DialogsMaintainer.addWhenShow(bean.context, dialog);
                //
                if (bean.type == DefaultConfig.TYPE_IOS_LOADING || bean.type == DefaultConfig.TYPE_MD_LOADING) {
                    // 这个方法与addWhenShow()有点像, 它是将activity与dialog保存在另一个Map集合中.
                    DialogsMaintainer.addLoadingDialog(bean.context, dialog);
                }
            }
        });

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog0) {
                if (bean.type == DefaultConfig.TYPE_IOS_INPUT) {
                    IosAlertDialogHolder iosAlertDialogHolder = (IosAlertDialogHolder) bean.viewHolder;
                    if (iosAlertDialogHolder != null) {
                        iosAlertDialogHolder.hideKeyBoard();
                    }
                }
                if (bean.listener != null) {
                    bean.listener.onCancle();
                }
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog0) {
                if (bean.listener != null) {
                    bean.listener.onDismiss();
                }
                // 这里是将Dialog与对应的Activity从Map集合中删除掉
                DialogsMaintainer.removeWhenDismiss(dialog);
                if (bean.type == DefaultConfig.TYPE_IOS_LOADING || bean.type == DefaultConfig.TYPE_MD_LOADING) {
                    DialogsMaintainer.dismissLoading(dialog);

                }
            }
        });


    }

    private static void setTitleMessageStyle(final Dialog dialog, ConfigBean bean) {

        try {
            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
            mAlert.setAccessible(true);
            Object mAlertController = mAlert.get(dialog);


            Field mMessage = mAlertController.getClass().getDeclaredField("mMessageView");
            mMessage.setAccessible(true);
            TextView tvMessage = (TextView) mMessage.get(mAlertController);
            if (bean.msgTxtColor != 0) {
                tvMessage.setTextColor(getColor(bean.context, bean.msgTxtColor));
            }
            if (bean.msgTxtSize != 0) {
                tvMessage.setTextSize(bean.msgTxtSize);
            }

            Field titleView = mAlertController.getClass().getDeclaredField("mTitleView");
            titleView.setAccessible(true);
            TextView tvTitle = (TextView) titleView.get(mAlertController);
            if (bean.titleTxtColor != 0) {
                tvTitle.setTextColor(getColor(bean.context, bean.titleTxtColor));
            }
            if (bean.titleTxtSize != 0) {
                tvTitle.setTextSize(bean.titleTxtSize);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 该方法是用来调整窗体大小的
    private static void adjustWindow(final Dialog dialog, final ConfigBean bean) {
        // addOnGlobalLayoutListener当视图可见性改变时候,将回调onGlobalLayout该方法.
        dialog.getWindow().getDecorView().getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // 设置BootmSheetDialog
                        setBottomSheetDialogPeekHeight(bean);
                        // 调整宽高
                        adjustWH(dialog, bean);
                        dialog.getWindow().getDecorView().getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                });
    }

    // 这个方法是根据Dialog配置对象中的gravity变量来设置Dialog进出场动画的.
    public static void setWindowAnimation(Window window, ConfigBean bean) {
        int gravity = bean.gravity;
        if (gravity == Gravity.BOTTOM || gravity == (Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL)) {
            window.setWindowAnimations(R.style.ani_bottom);
        } else if (gravity == Gravity.CENTER) {
            //window.setWindowAnimations(R.style.dialog_center);
        }
    }


    public static void runOnUIThread(Runnable runnable) {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        mainHandler.post(runnable);
    }

    public static void runOnUIThreadDelayed(Runnable runnable) {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        mainHandler.postDelayed(runnable, 500);
    }

    public static void showSoftKeyBoardDelayed(boolean sholudShouldKeyBoard, final SuperLvHolder holder) {
        if (!sholudShouldKeyBoard) {
            return;
        }
        if (holder == null) {
            return;
        }
        /*if(mainHandler ==null){
            mainHandler = new Handler(Looper.getMainLooper());
        }
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                holder.showKeyBoard();
            }
        }, 0);*/
        holder.showKeyBoard();
    }


    /**
     * 必须在show之后,button才不会返回null
     *
     * @param bean
     */
    public static void setMdBtnStytle(final ConfigBean bean) {
        Button btnPositive =
                bean.alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button btnNegative =
                bean.alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        Button btnNatural =
                bean.alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);

        if (btnPositive != null) {
            btnPositive.setAllCaps(false);
            if (!TextUtils.isEmpty(bean.text1)) {
                btnPositive.setText(bean.text1);
            }
            if (bean.btn1Color > 0) {
                btnPositive.setTextColor(getColor(bean.context, bean.btn1Color));
            }

            if (bean.btnTxtSize > 0) {
                btnPositive.setTextSize(bean.btnTxtSize);
            }
            btnPositive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (bean.type == DefaultConfig.TYPE_MD_INPUT) {
                        MdInputHolder holder = (MdInputHolder) bean.viewHolder;
                        boolean isvalid = bean.listener.onInputValid(holder.getTxt1(), holder.getTxt2(), holder.getEt1(), holder.getEt2());
                        if (!isvalid) {
                            return;
                        }
                        bean.listener.onGetInput(holder.getTxt1(), holder.getTxt2());
                    } else if (bean.type == DefaultConfig.TYPE_MD_SINGLE_CHOOSE) {

                    } else if (bean.type == DefaultConfig.TYPE_MD_MULTI_CHOOSE) {
                        bean.listener.onGetChoose(bean.checkedItems);
                        List<Integer> selectedIndex = new ArrayList<Integer>();
                        List<CharSequence> selectedStrs = new ArrayList<CharSequence>();
                        for (int j = 0; j < bean.checkedItems.length; j++) {
                            if (bean.checkedItems[j]) {
                                selectedIndex.add(j);
                                selectedStrs.add(bean.wordsMd[j]);
                            }
                        }
                        bean.listener.onChoosen(selectedIndex, selectedStrs, bean.checkedItems);
                    }
                    bean.listener.onFirst();
                    dismiss(bean, true);
                }
            });
        }
        if (btnNegative != null) {
            btnNegative.setAllCaps(false);
            if (!TextUtils.isEmpty(bean.text2)) {
                btnNegative.setText(bean.text2);
            }
            if (bean.btn2Color > 0) {
                if (bean.btn2Color == DefaultConfig.iosBtnColor) {
                    btnNegative.setTextColor(getColor(bean.context, R.color.dialogutil_text_gray));
                } else {
                    btnNegative.setTextColor(getColor(bean.context, bean.btn2Color));
                }
            }


            if (bean.btnTxtSize > 0) {
                btnNegative.setTextSize(bean.btnTxtSize);
            }
        }

        if (btnNatural != null) {
            btnNatural.setAllCaps(false);
            if (!TextUtils.isEmpty(bean.text3)) {
                btnNatural.setText(bean.text3);
            }
            if (bean.btn3Color > 0) {
                btnNatural.setTextColor(getColor(ActivityStackManager.getInstance().getTopActivity(), bean.btn3Color));
            }

            if (bean.btnTxtSize > 0) {
                btnNatural.setTextSize(bean.btnTxtSize);
            }
        }


    }

    public static ConfigBean fixContext(ConfigBean bean) {
        Activity activity1 = null;
        if (!(bean.context instanceof Activity)) {
            // 假如传入的context不是输入Activity的,那么就取一个最顶层的Activity作为这个Dialog配置对象也就是ConfigBean对象的变量context的新值.
            Activity activity = ActivityStackManager.getInstance().getTopActivity();
            if (isUsable(activity)) {
                // 假如这个Activity是可以用的, 那么这个Activity对象就是ConfigBean对象的变量context的新值.
                activity1 = activity;
            }
        } else {
            // 假如传入的就是一个Activity对象
            Activity activity = (Activity) bean.context;
            if (isUsable(activity)) {
                // 假如这个Activity是可以用的, 那么这个Activity对象就是ConfigBean对象的变量context的新值.
                activity1 = activity;
            }
        }
        if (activity1 != null) {
            // 为Dialog 配置对象中的context变量赋值
            bean.context = activity1;
        } else {
            // StyledDialog.init(getApplicationContext());
            // StyledDialog.context 这是ApplicationContext对象
            bean.context = StyledDialog.context;
        }

        Log.e("tool", "fixContext:" + bean.context);
        return bean;
    }

    /**
     * 判断Activity是否可用
     *
     * @param activity
     * @return
     */
    public static boolean isUsable(Activity activity) {
        // 是否为空
        if (activity == null) {
            return false;
        }
        // 是否被finish
        if (activity.isFinishing()) {
            return false;
        }
        // 是否已经Destroyed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (activity.isDestroyed()) {
                return false;
            }
        }
        return true;
    }


    /**
     * ViewRootImpl.checkThread:只有创建View层次结构的线程才能修改View
     * 如何保证new Dialog在UI线程执行?或者说,保证构建dialog view的线程和dismiss在同一线程?
     * 查看源码知道,dismiss回强制在UI线程,所以,构建dialogview的线程也必须是UI线程.
     * 什么时候构建dialog的viewroot?
     * 普通dialog: new Dialog()方法中: Window w = new PhoneWindow(mContext)
     * alertdialog: dialogbuilder.build()方法中
     *
     * @param bean
     * @return
     */
    public static ConfigBean newCustomDialog(ConfigBean bean) {
        // 真正的根据context变量来创建Dialog对象
        Dialog dialog = new Dialog(bean.context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        bean.dialog = dialog;
        return bean;
    }

    public static ConfigBean setCancelable(ConfigBean bean) {

        if (bean.alertDialog != null) {
            bean.alertDialog.setCancelable(bean.cancelable);
            bean.alertDialog.setCanceledOnTouchOutside(bean.outsideTouchable);
            // bean.alertDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else if (bean.dialog != null) {
            bean.dialog.setCancelable(bean.cancelable);
            bean.dialog.setCanceledOnTouchOutside(bean.outsideTouchable);
            //  bean.dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
        return bean;
    }


    public static void adjustStyle(final ConfigBean bean) {
        // 设置背景
        setBg(bean);
        // 设置窗口的暗淡
        setDim(bean);
        Dialog dialog = bean.dialog == null ? bean.alertDialog : bean.dialog;
        // 获取Dialog对应的窗口
        Window window = dialog.getWindow();
        // 设置窗口的位置
        window.setGravity(bean.gravity);
        if (bean.context instanceof Activity) {
            //setHomeKeyListener(window,bean);
        } else {
            // 这里其实就是为当创建的Dialog中context是Application Context的时候所做的事情
            // 设置窗体的类型为toast类型, 因为toast类型是不需要token的,他是系统的
            window.setType(WindowManager.LayoutParams.TYPE_TOAST);
            // 获取Dialog窗体的的参数
            WindowManager.LayoutParams params = window.getAttributes();
            if (params == null) {
                // 如果没有参数就设置一个
                // 其实这个不可能为null的, 源码中知道每次创建窗体都会默认创建一个WindowManager.LayoutParams对象出来的.
                params = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            // 设置位图格式
            params.format = PixelFormat.RGBA_8888;
            params.flags =
                    // WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL  |
                    WindowManager.LayoutParams.FLAG_LOCAL_FOCUS_MODE |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
            params.dimAmount = 0.2f;
            //params.alpha = 0.5f;//the alpha of window
            // 为窗体设置参数
            window.setAttributes(params);
            // 为窗体设置回调返回按键的回调监听
            // back key pressed
            window.getDecorView().setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) {
                        StyledDialog.dismiss(bean.alertDialog, bean.dialog);
                        return true;
                    }
                    return false;
                }
            });
            // 监听Home案件广播?
            setHomeKeyListener(window, bean);
            window.setDimAmount(0.2f);
        }

    }

    private static void setHomeKeyListener(final Window window, final ConfigBean bean) {
        //在创建View时注册Receiver
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        bean.homeKeyReceiver = new BroadcastReceiver() {
            final String SYSTEM_DIALOG_REASON_KEY = "reason";
            final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                    String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                    if (reason != null && reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                        // 处理自己的逻辑
                        if (bean.type == DefaultConfig.TYPE_IOS_INPUT) {
                            hideKeyBoard(window);
                        }
                        if (!(bean.context instanceof Activity)) {
                            Tool.dismiss(bean);
                        }
                        context.unregisterReceiver(this);
                    }
                }
            }
        };
        bean.context.registerReceiver(bean.homeKeyReceiver, homeFilter);
    }


    private static void setDim(ConfigBean bean) {
        if (bean.type == DefaultConfig.TYPE_IOS_LOADING) {//转菊花,则让背景透明
            bean.isTransparentBehind = true;
        }
        if (bean.alertDialog != null) {
            if (bean.isTransparentBehind) {
                bean.alertDialog.getWindow().setDimAmount(0);
            }
        } else {
            if (bean.isTransparentBehind) {
                bean.dialog.getWindow().setDimAmount(0);
            }
        }
    }

    /**
     * set background of dialog window
     *
     * @param bean
     */
    private static void setBg(ConfigBean bean) {
        //no need to modify the background
        if ((bean.type == DefaultConfig.TYPE_BOTTOM_SHEET_GRID && bean.hasBehaviour)
                || bean.type == DefaultConfig.TYPE_BOTTOM_SHEET_LIST
                || bean.type == DefaultConfig.TYPE_BOTTOM_SHEET_CUSTOM
                || bean.type == DefaultConfig.TYPE_PROGRESS) {
            // No need to set backgroud
            return;

        }


        if (bean.alertDialog != null) {

            if (bean.useTheShadowBg) {
                bean.alertDialog.getWindow().setBackgroundDrawableResource(R.drawable.shadow);
            } else {
                if (bean.bgRes > 0)
                    bean.alertDialog.getWindow().setBackgroundDrawableResource(bean.bgRes);
                else {
                    bean.alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }

            }
        } else {
            if (bean.type == DefaultConfig.TYPE_IOS_LOADING) {//转菊花时,背景透明
                bean.dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            } else if ((bean.type == DefaultConfig.TYPE_BOTTOM_SHEET_GRID && !bean.hasBehaviour)) {
                bean.dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            } else {
                if (bean.useTheShadowBg) {
                    bean.dialog.getWindow().setBackgroundDrawableResource(R.drawable.shadow);
                } else {
                    if (bean.bgRes > 0)
                        bean.dialog.getWindow().setBackgroundDrawableResource(bean.bgRes);
                    else {
                        bean.dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }
                }
            }

        }


    }

    // 该方法调整Dialog窗体宽高
    public static void adjustWH(Dialog dialog, ConfigBean bean) {
        if (dialog == null) {
            return;
        }
        // 获取Dialog窗体
        Window window = dialog.getWindow();
        // 获取Dialog 中的DecorView对象
        View rootView = window.getDecorView();
        // 获取Dialog对应Window的参数
        WindowManager.LayoutParams wl = window.getAttributes();
        // 获取屏幕宽高
        int width = window.getWindowManager().getDefaultDisplay().getWidth();
        int height = window.getWindowManager().getDefaultDisplay().getHeight();
        // 获取Dialog中的DecorView宽高
        int measuredHeight = rootView.getMeasuredHeight();
        int measuredWidth = rootView.getMeasuredWidth();

        float widthRatio = 0.85f;
        float heightRatio = 0f;
        if (bean.type == DefaultConfig.TYPE_IOS_BOTTOM) {
            widthRatio = 0.95f;
        } else if (bean.type == DefaultConfig.TYPE_IOS_CENTER_LIST) {
            widthRatio = 0.9f;
        }
        if (width > height) {//宽屏
            widthRatio = 0.5f;
        }

        //set ratio as user has set
        if (bean.forceWidthPercent > 0 && bean.forceWidthPercent <= 1.0f) {
            widthRatio = bean.forceWidthPercent;
        }
        if (measuredHeight > bean.maxHeightPercent * height) {
            heightRatio = bean.maxHeightPercent;
        }
        if (bean.forceHeightPercent > 0 && bean.forceHeightPercent <= 1.0f) {
            heightRatio = bean.forceHeightPercent;
        }

        if (istheTypeOfNotAdjust(bean)) {
            /*wl.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;*/

        } else {
            // 这里就是为 Dialog 对应的 layoutParams的w或者h设置新得宽高
            // rootView.setPadding(0,30,0,30);
            wl.width = (int) (width * widthRatio);//stretch when the content is not enough,margin when the content is full fill the screen
            //if (measuredHeight > height* heightRatio){//only work when the content is full fill the screen
            if (heightRatio > 0) {
                wl.height = (int) (height * heightRatio);
            }

            if (bean.type == DefaultConfig.TYPE_BOTTOM_SHEET_GRID && !bean.hasBehaviour) {
                wl.height = measuredHeight;
            }
        }
        // 通知WindowManager重新layout布局
        dialog.onWindowAttributesChanged(wl);
    }

    public static boolean istheTypeOfNotAdjust(ConfigBean bean) {
        switch (bean.type) {
            case DefaultConfig.TYPE_IOS_LOADING:
            case DefaultConfig.TYPE_PROGRESS:
            case DefaultConfig.TYPE_BOTTOM_SHEET_CUSTOM:
            case DefaultConfig.TYPE_BOTTOM_SHEET_LIST:
                // case DefaultConfig.TYPE_CUSTOM_VIEW:
                return true;
            case DefaultConfig.TYPE_BOTTOM_SHEET_GRID:
                return bean.hasBehaviour;
            default:
                return false;
        }
    }

    private static boolean isCustomType(ConfigBean bean) {
        switch (bean.type) {
            case DefaultConfig.TYPE_IOS_HORIZONTAL:
            case DefaultConfig.TYPE_IOS_VERTICAL:
            case DefaultConfig.TYPE_IOS_BOTTOM:
            case DefaultConfig.TYPE_IOS_CENTER_LIST:
            case DefaultConfig.TYPE_IOS_INPUT:
            case DefaultConfig.TYPE_CUSTOM_VIEW:
            case DefaultConfig.TYPE_MD_LOADING:
            case DefaultConfig.TYPE_MD_ALERT:
            case DefaultConfig.TYPE_MD_MULTI_CHOOSE:
            case DefaultConfig.TYPE_MD_SINGLE_CHOOSE:
                return true;
            default:
                return false;
        }
    }


    public static void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    ,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int lpHeight = p.height;
        int lpWidth = p.width;
        int childHeightSpec;
        int childWidthSpec;
        if (lpHeight > 0) {   //如果Height是一个定值，那么我们测量的时候就使用这个定值
            childHeightSpec = View.MeasureSpec.makeMeasureSpec(lpHeight,
                    View.MeasureSpec.EXACTLY);
        } else {  // 否则，我们将mode设置为不指定，size设置为0
            childHeightSpec = View.MeasureSpec.makeMeasureSpec(0,
                    View.MeasureSpec.UNSPECIFIED);
        }
        if (lpWidth > 0) {
            childWidthSpec = View.MeasureSpec.makeMeasureSpec(lpHeight,
                    View.MeasureSpec.EXACTLY);
        } else {
            childWidthSpec = View.MeasureSpec.makeMeasureSpec(0,
                    View.MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    /**
     * @param root
     * @param id   height为0,weight为1的scrollview包裹的view的id,如果没有,传0或负数即可
     * @return
     */
    public static int mesureHeight(View root, int id) {
        measureView(root);
        int height = root.getMeasuredHeight();
        int heightExtra = 0;
        if (id > 0) {
            View view = root.findViewById(id);
            if (view != null) {
                measureView(view);
                heightExtra = view.getMeasuredHeight();
            }

        }
        return height + heightExtra;
    }

    public static int mesureHeight(View root, View... subViews) {
        measureView(root);
        int height = root.getMeasuredHeight();
        int heightExtra = 0;
        if (subViews != null && subViews.length > 0) {
            for (View view : subViews) {
                if (view.getVisibility() == View.VISIBLE) {//确保设置了gone的view不再出现
                    measureView(view);
                    heightExtra += view.getMeasuredHeight();
                }

            }

        }

        return height + heightExtra;
    }

    public static int getColor(Context context, int colorRes) {
        if (context == null) {
            context = StyledDialog.context;
        }
        try {
            int color = context.getResources().getColor(colorRes);
            return color;
        } catch (Exception e) {
            e.printStackTrace();
            return Color.TRANSPARENT;
        }


    }


    public static void handleScrollInBottomSheetDialog(final AdapterView listView) {
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!listView.canScrollVertically(-1)) {      //canScrollVertically(-1)的值表示是否能向下滚动，false表示已经滚动到顶部
                    listView.requestDisallowInterceptTouchEvent(false);
                } else {
                    listView.requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });
    }


    /**
     * 设置BottomSheetDialog的初始最大高度,解决dismiss后无法再show的问题
     *
     * @param bean
     */
    public static void setBottomSheetDialogPeekHeight(final ConfigBean bean) {
        // dialog类型是BottomSheetDialog才会执行该方法
        if (bean.hasBehaviour && bean.dialog instanceof BottomSheetDialog) {
            View view = bean.dialog.getWindow().findViewById(android.support.design.R.id.design_bottom_sheet);
            if (view == null) {
                return;
            }

            final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(view);
            bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    // 如果是隐藏的话
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        Tool.dismiss(bean);
                        // 看了源码发现很简单，我们自己来实现，并且在监听到用户滑动关闭BottomSheetDialog后，我们把BottomSheetBehavior的状态设置为BottomSheetBehavior.STATE_COLLAPSED，也就是半个打开状态（BottomSheetBehavior.STATE_EXPANDED为全打开）
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                }
            });

            if (bean.bottomSheetDialogMaxHeightPercent > 0f && bean.bottomSheetDialogMaxHeightPercent < 1f) {
                // 通过百分比,获取BottomSheetDialog的最大高度.
                int peekHeight = (int) (bean.bottomSheetDialogMaxHeightPercent * ScreenUtil.getScreenHeight());
                // 将高度设置给BottomSheetBehavior
                bottomSheetBehavior.setPeekHeight(peekHeight);
            }
        }
    }

    public static void showKeyBoard(View edCount) {

        //设置可获得焦点
        edCount.setFocusable(true);
        edCount.setFocusableInTouchMode(true);
        //请求获得焦点
        edCount.requestFocus();
        //调用系统输入法
        InputMethodManager imm = (InputMethodManager) StyledDialog.context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(edCount, InputMethodManager.RESULT_SHOWN);
    }

    public static void hideKeyBorad(ConfigBean bean) {
        if (!bean.needSoftKeyboard) {
            return;
        }
        if (bean.viewHolder != null) {
            bean.viewHolder.hideKeyBoard();
        }
        if (bean.customContentHolder != null) {
            bean.customContentHolder.hideKeyBoard();
        }
    }


    public static void hideKeyBoard(View view) {
        InputMethodManager imm = (InputMethodManager) StyledDialog.context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0); //强制隐藏键盘
    }

    public static void hideKeyBoard(Window window) {
        InputMethodManager imm = (InputMethodManager) StyledDialog.context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(window.getDecorView().getWindowToken(), 0); //强制隐藏键盘
    }


    public static void removeFromParent(View customView) {
        ViewParent parent = customView.getParent();
        if (parent != null) {
            ViewGroup group = (ViewGroup) parent;
            group.removeView(customView);
        }
    }
}
