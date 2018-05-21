package task.message;


import com.yyz.CurrencyThreadTask.task.message.interfaces.IEnvelope;
import com.yyz.CurrencyThreadTask.task.message.interfaces.IMsgCourier;
import com.yyz.CurrencyThreadTask.task.message.interfaces.IMsgPostOffice;
import com.yyz.CurrencyThreadTask.task.message.interfaces.INotifyListener;
import com.yyz.CurrencyThreadTask.task.utils.ThreadAnnotation;

import java.util.Enumeration;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * MessageCourier 线程消息收发者
 *
 * @author yyz
 * @date 4/10/2017.
 * Created by prolog on 4/10/2017.
 */
public class MessageCourier implements IMsgCourier {
    private INotifyListener listener;
    private Object target = null;
    private final String courierKey;
    /***消息栈*/
    private Queue<IEnvelope> msgStack = new ConcurrentLinkedQueue();
    private final Stack<IMsgPostOffice> serverList = new Stack<>();

    public MessageCourier(INotifyListener listener) {
        if (listener == null) {
            throw new NullPointerException("listener cannot be null");
        }
        courierKey = toString();
        this.listener = listener;
    }

    public MessageCourier(Object target) {
        if (target == null) {
            throw new NullPointerException("target cannot be null");
        }
        courierKey = toString();
        this.target = target;
    }

    /**
     * //线程间通信接口，其它线程发送数据可以在这里收到
     *
     * @param message 传递的数据
     */
    @Override
    public void onReceiveEnvelope(IEnvelope message) {
        switch (message.getType()) {
            //即时消息
            case INSTANT:
            default:
                if (listener == null) {
                    ThreadAnnotation.disposeMessage(message.getMethodName(), target, message);
                } else {
                    listener.onInstantMessage(message);
                }
                break;
            //非即时消息
            case MAIL:
                msgStack.add(message);
                break;
        }
    }

    @Override
    public void removeEnvelopeServer(IMsgPostOffice sender) {
        serverList.remove(sender);
    }

    @Override
    public String getCourierKey() {
        return courierKey;
    }

    /**
     * 取出消息
     *
     * @return 返回消息
     */
    public IEnvelope popMessage() {
        IEnvelope data = null;
        if (!msgStack.isEmpty()) {
            data = msgStack.remove();
        }
        return data;
    }

    /**
     * 设置消息发送者
     *
     * @param postOffice 消息发送者
     */
    @Override
    public void setEnvelopeServer(IMsgPostOffice postOffice) {
        if (serverList.contains(postOffice) == false) {
            serverList.add(postOffice);
            postOffice.registeredListener(this);
        }
    }


    /**
     * 发送消息（使用代理传递消息）
     *
     * @param message 消息
     */
    @Override
    public void sendEnvelopeProxy(IEnvelope message) {
        if (message != null) {
            message.setHighOverhead(true);
            sendEnvelop(message);
        }
    }

    private void sendEnvelop(IEnvelope message) {
        if (message.getSenderKey() == null) {
            message.setSenderKey(courierKey);
        }
        Enumeration<IMsgPostOffice> enumeration = serverList.elements();
        while (enumeration.hasMoreElements()) {
            IMsgPostOffice sender = enumeration.nextElement();
            sender.sendEnvelope(message);
        }
    }

    /**
     * 发送消息(传递消息不用代理)
     *
     * @param message 消息
     */
    @Override
    public void sendEnvelopSelf(IEnvelope message) {
        if (message != null) {
            message.setHighOverhead(false);
            sendEnvelop(message);
        }
//        ThreadAnnotation.disposeMessage(target, message, message.getAnnotation());
    }

    /**
     * 释放资源
     */
    @Override
    public void release() {
        Enumeration<IMsgPostOffice> enumeration = serverList.elements();
        while (enumeration.hasMoreElements()) {
            IMsgPostOffice sender = enumeration.nextElement();
            sender.unRegisteredListener(this);
        }
        serverList.clear();
        msgStack.clear();
    }
}
