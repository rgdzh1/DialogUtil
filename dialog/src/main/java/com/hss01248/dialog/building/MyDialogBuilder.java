package com.hss01248.dialog.building;


import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.hss01248.dialog.R;
import com.hss01248.dialog.Tool;
import com.hss01248.dialog.bottomsheet.BottomSheetHolder;
import com.hss01248.dialog.bottomsheet.RightMdBottomSheetDialog;
import com.hss01248.dialog.config.BottomSheetStyle;
import com.hss01248.dialog.config.ConfigBean;
import com.hss01248.dialog.config.DefaultConfig;
import com.hss01248.dialog.ios.IosActionSheetHolder;
import com.hss01248.dialog.ios.IosAlertDialogHolder;
import com.hss01248.dialog.ios.IosCenterItemHolder;
import com.hss01248.dialog.material.MaterialDialogHolder;
import com.hss01248.dialog.material.MdInputHolder;
import com.hss01248.dialog.view.AdXHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/10/9.
 */
public class MyDialogBuilder {

    protected static int singleChosen;

    protected ConfigBean buildByType(ConfigBean bean) {
        Tool.fixContext(bean);
        switch (bean.type) {
            case DefaultConfig.TYPE_MD_LOADING:
                Tool.newCustomDialog(bean);// 创建Dialog
                buildMdLoading(bean);// 内容填充
                break;
            case DefaultConfig.TYPE_PROGRESS:
                buildProgress(bean);
                break;
            case DefaultConfig.TYPE_MD_ALERT:
            case DefaultConfig.TYPE_MD_INPUT:
                if (bean.context instanceof Activity && !bean.showAsActivity) {
                    buildMdAlert(bean);
                } else {
                    buildMyMd(bean);
                }

                break;
            case DefaultConfig.TYPE_MD_SINGLE_CHOOSE:
                if (bean.context instanceof Activity && !bean.showAsActivity) {
                    buildMdSingleChoose(bean);
                } else {
                    buildMyMd(bean);
                }

                break;
            case DefaultConfig.TYPE_MD_MULTI_CHOOSE:
                if (bean.context instanceof Activity && !bean.showAsActivity) {
                    buildMdMultiChoose(bean);
                } else {
                    buildMyMd(bean);
                }

                break;
            case DefaultConfig.TYPE_IOS_HORIZONTAL:
                Tool.newCustomDialog(bean);
                buildIosAlert(bean);
                break;
            case DefaultConfig.TYPE_IOS_VERTICAL:
                Tool.newCustomDialog(bean);
                buildIosAlertVertical(bean);
                break;
            case DefaultConfig.TYPE_IOS_BOTTOM:
                Tool.newCustomDialog(bean);
                buildBottomItemDialog(bean);
                break;
            case DefaultConfig.TYPE_IOS_INPUT:
                Tool.newCustomDialog(bean);
                buildNormalInput(bean);
                break;
            case DefaultConfig.TYPE_IOS_CENTER_LIST:
                Tool.newCustomDialog(bean);
                buildIosSingleChoose(bean);
                break;
            case DefaultConfig.TYPE_CUSTOM_VIEW:
                Tool.newCustomDialog(bean);
                View rootView = getCustomRootView(bean);
                bean.dialog.setContentView(rootView);
                break;
            case DefaultConfig.TYPE_BOTTOM_SHEET_CUSTOM:
                buildBottomSheet(bean);


                break;
            case DefaultConfig.TYPE_BOTTOM_SHEET_LIST:
                buildBottomSheetLv(bean);


                break;
            case DefaultConfig.TYPE_BOTTOM_SHEET_GRID:
                buildBottomSheetLv(bean);

                break;

            case DefaultConfig.TYPE_IOS_LOADING:
                // 就是根据不同的Dialog配置对象类型风格,创建真正的Dialog对象,
                // 创建的Dilaog对象又放入进ConfigBean对象中了.
                Tool.newCustomDialog(bean);
                // 这里的一步就是对Dialog中进行内容的填充,比如你想要的文字,动画等等.
                buildLoading(bean);
                break;
            default:
                break;


        }
        Dialog dialog = bean.dialog == null ? bean.alertDialog : bean.dialog;
        // 获取dialog所在的窗口, 这个是在创建Dialog时候创建的.
        Window window = dialog.getWindow();
        // 设置窗口半透明
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // 这个方法是根据Dialog配置对象中的gravity变量来设置Dialog进出场动画的.
        Tool.setWindowAnimation(window, bean);
        // 这个方法是根据Dialog配置对象中的cancelable变量来设置Dialog是否触摸到外部直接隐藏Dialog的
        Tool.setCancelable(bean);
        // 设置很多相关的监听, 主要是将Dialog与Activity用Map集合相应的保管起来
        Tool.setListener(dialog, bean);
        // 调整很多参数
        Tool.adjustStyle(bean);
        return bean;
    }

    private View getCustomRootView(ConfigBean bean) {

        if (bean.customContentHolder != null) {
            Tool.removeFromParent(bean.customContentHolder.rootView);
            if (bean.asAdXStyle) {
                AdXHolder adXHolder = new AdXHolder(bean.context);
                adXHolder.assingDatasAndEvents(bean.context, bean);
                return adXHolder.rootView;
            }
            return bean.customContentHolder.rootView;
        } else {
            Tool.removeFromParent(bean.customView);
            if (bean.asAdXStyle) {
                AdXHolder adXHolder = new AdXHolder(bean.context);
                adXHolder.assingDatasAndEvents(bean.context, bean);
                return adXHolder.rootView;
            }
            return bean.customView;

        }
    }

    private void buildMyMd(ConfigBean bean) {
        Tool.newCustomDialog(bean);
        MaterialDialogHolder holder = new MaterialDialogHolder(bean.context);
        bean.viewHolder = holder;
        holder.assingDatasAndEvents(bean.context, bean);
        bean.dialog.setContentView(holder.rootView);
    }


    private void buildProgress(ConfigBean bean) {
        ProgressDialog dialog = new ProgressDialog(bean.context);
        dialog.setTitle("");
        dialog.setMessage(bean.msg);
        dialog.setProgressStyle(bean.isProgressHorzontal ? ProgressDialog.STYLE_HORIZONTAL : ProgressDialog.STYLE_SPINNER);
        dialog.setIndeterminate(false);
        bean.dialog = dialog;
    }


    private void buildBottomSheetLv(final ConfigBean bean) {
        Dialog dialog = null;
        if (bean.hasBehaviour) {
            dialog = new RightMdBottomSheetDialog(bean.context);
            bean.forceWidthPercent = 1.0f;
        } else {
            Tool.newCustomDialog(bean);
            dialog = bean.dialog;
            bean.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            bean.forceWidthPercent = 1.0f;
            bean.bgRes = R.color.dialogutil_bg_white;
        }
        bean.dialog = dialog;


        if (bean.bottomSheetStyle == null) {
            bean.bottomSheetStyle = BottomSheetStyle.newBuilder().build();
        }

        BottomSheetHolder bottomSheetHolder = new BottomSheetHolder(bean.context);
        bottomSheetHolder.assingDatasAndEvents(bean.context, bean);
        bean.viewHolder = bottomSheetHolder;

        dialog.setContentView(bottomSheetHolder.rootView);


    }

    private void buildBottomSheet(ConfigBean bean) {
        final BottomSheetDialog dialog = new BottomSheetDialog(bean.context);
        Tool.removeFromParent(bean.customView);
        dialog.setContentView(bean.customView);
        bean.forceWidthPercent = 1.0f; // 占比宽度是多少
        dialog.setCancelable(bean.cancelable);
        dialog.setCanceledOnTouchOutside(bean.outsideTouchable);
        bean.dialog = dialog;
    }

    protected ConfigBean buildLoading(ConfigBean bean) {
        // 这里的一步就是对Dialog中进行内容的填充,比如你想要的文字,动画等等.
        View root = View.inflate(bean.context, R.layout.loading, null);
        ImageView gifMovieView = (ImageView) root.findViewById(R.id.iv_loading);
        AnimationDrawable drawable = (AnimationDrawable) gifMovieView.getDrawable();
        if (drawable != null) {
            drawable.start();
        }
        TextView tvMsg = (TextView) root.findViewById(R.id.loading_msg);
        tvMsg.setText(bean.msg);
        bean.dialog.setContentView(root);
        return bean;
    }


    protected ConfigBean buildMdLoading(ConfigBean bean) {
        View root = View.inflate(bean.context, R.layout.progressview_wrapconent, null);
        TextView tvMsg = (TextView) root.findViewById(R.id.loading_msg);
        tvMsg.setText(bean.msg);
        bean.dialog.setContentView(root);
        return bean;
    }

    protected ConfigBean buildMdAlert(final ConfigBean bean) {
        Tool.fixContext(bean);
        final AlertDialog.Builder builder = new AlertDialog.Builder(bean.context);
        if (bean.customContentHolder == null) {
            if (bean.type == DefaultConfig.TYPE_MD_INPUT) {
                MdInputHolder holder = new MdInputHolder(bean.context);
                bean.viewHolder = holder;
                bean.setNeedSoftKeyboard(true);
                holder.assingDatasAndEvents(bean.context, bean);
                builder.setView(bean.viewHolder.rootView);
            } else {
                builder.setMessage(bean.msg);
            }

        } else {
            builder.setView(bean.customContentHolder.rootView);
            //bean.customContentHolder.assingDatasAndEvents(Tool.fixContext(bean).context,bean);
        }
        builder.setTitle(bean.title)
                .setPositiveButton(bean.text1, null)//不让点击默认消失，而是做出判断，见Tool.setMdBtnStytle
                .setNegativeButton(bean.text2, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        bean.listener.onSecond();
                        Tool.hideKeyBorad(bean);
                        dialog.dismiss();
                    }
                }).setNeutralButton(bean.text3, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                bean.listener.onThird();
                Tool.hideKeyBorad(bean);
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Tool.hideKeyBorad(bean);
                if (bean.listener != null)
                    bean.listener.onCancle();
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (bean.listener != null) {
                    bean.listener.onDismiss();
                }
            }
        });
        bean.alertDialog = dialog;
        return bean;
    }

    protected ConfigBean buildMdSingleChoose(final ConfigBean bean) {
        AlertDialog.Builder builder = new AlertDialog.Builder(bean.context);
        singleChosen = bean.defaultChosen;
        builder.setTitle(bean.title)
                .setPositiveButton(bean.text1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (bean.listener != null) {
                            bean.listener.onFirst();
                            bean.listener.onGetChoose(singleChosen, bean.wordsMd[singleChosen]);
                        }
                        Tool.dismiss(bean, true);
                    }
                })
                .setNegativeButton(bean.text2, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (bean.listener != null) {
                            bean.listener.onSecond();
                        }
                        Tool.dismiss(bean);
                    }
                })
                .setSingleChoiceItems(bean.wordsMd, bean.defaultChosen, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        singleChosen = i;
                        if (bean.itemListener != null) {
                            bean.itemListener.onItemClick(bean.wordsMd[i], i);
                        }
                        Tool.dismiss(bean, true);

                    }
                });

        AlertDialog dialog = builder.create();// crash when context is not activity
        bean.alertDialog = dialog;
        //dialog.getWindow().getDecorView()
        // addShaow(bean,dialog);


        return bean;
    }


    protected ConfigBean buildMdMultiChoose(final ConfigBean bean) {
        AlertDialog.Builder builder = new AlertDialog.Builder(bean.context);
        builder.setTitle(bean.title)
                .setCancelable(true)
                .setPositiveButton(bean.text1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (bean.listener != null) {
                            bean.listener.onFirst();
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
                        Tool.dismiss(bean, true);
                    }
                })
                .setNegativeButton(bean.text2, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (bean.listener != null) {
                            bean.listener.onSecond();
                        }
                        Tool.dismiss(bean);
                    }
                })
                .setMultiChoiceItems(bean.wordsMd, bean.checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {

                    }
                })
        ;

        AlertDialog dialog = builder.create();
        bean.alertDialog = dialog;
        return bean;
    }

    protected ConfigBean buildIosAlert(ConfigBean bean) {
        bean.isVertical = false;
        bean.hint1 = "";
        bean.hint2 = "";
        buildIosCommon(bean);
        return bean;
    }

    protected ConfigBean buildIosAlertVertical(ConfigBean bean) {
        bean.isVertical = true;
        bean.hint1 = "";
        bean.hint2 = "";
        buildIosCommon(bean);
        return bean;
    }

    protected ConfigBean buildIosSingleChoose(ConfigBean bean) {
        IosCenterItemHolder holder = new IosCenterItemHolder(bean.context);
        bean.viewHolder = holder;

        bean.dialog.setContentView(holder.rootView);
        holder.assingDatasAndEvents(bean.context, bean);

        bean.viewHeight = Tool.mesureHeight(holder.rootView, holder.lv);

        Window window = bean.dialog.getWindow();
        window.setGravity(Gravity.CENTER);
        return bean;
    }

    protected ConfigBean buildBottomItemDialog(ConfigBean bean) {
        IosActionSheetHolder holder = new IosActionSheetHolder(bean.context);
        bean.viewHolder = holder;
        bean.dialog.setContentView(holder.rootView);

        holder.assingDatasAndEvents(bean.context, bean);

        bean.viewHeight = Tool.mesureHeight(holder.rootView, holder.lv);

        Window window = bean.dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.mystyle);
        return bean;
    }


    protected ConfigBean buildNormalInput(ConfigBean bean) {
        buildIosCommon(bean);
        return bean;
    }

    private ConfigBean buildIosCommon(ConfigBean bean) {

        IosAlertDialogHolder holder = new IosAlertDialogHolder(bean.context);
        bean.viewHolder = holder;
        bean.dialog.setContentView(holder.rootView);
        holder.assingDatasAndEvents(bean.context, bean);

        int height = Tool.mesureHeight(holder.rootView, holder.tvMsg, holder.et1, holder.et2);
        bean.viewHeight = height;


        return bean;

    }


}
