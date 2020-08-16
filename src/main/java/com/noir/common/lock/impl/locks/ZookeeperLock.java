package com.noir.common.lock.impl.locks;

import com.noir.common.lock.ReentrantDLock;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * zookeeper lock
 *
 * 依赖zk，发生死锁场景低，并发支持不如缓存
 */
public class ZookeeperLock extends ReentrantDLock implements Watcher {
    private final ZooKeeper zk;
    private final String root = "/locks";//根
    private final String lockName;//竞争资源的标志
    private String waitNode;//等待前一个锁
    private String myZNode;//当前锁
    private CountDownLatch latch;//计数器
    private final long sessionTimeout;

    public ZookeeperLock(ZooKeeper zk, String lockName) throws KeeperException, InterruptedException {
        this(zk, lockName, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public ZookeeperLock(ZooKeeper zk, String lockName, long expire, TimeUnit unit) throws KeeperException, InterruptedException {
        this.lockName = lockName;
        this.zk = zk;
        Stat stat = zk.exists(root, false);
        if(stat == null){
            // 创建根节点
            zk.create(root, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
        this.sessionTimeout = unit.toMillis(expire);
    }

    /**
     * zookeeper节点的监视器
     */
    public void process(WatchedEvent event) {
        if(this.latch != null) {
            this.latch.countDown();
        }
    }

    public void lock() {
        try {
            if(this.tryLock()) {
                System.out.println("Thread " + Thread.currentThread().getId() + " " + myZNode + " get lock true");
            } else {
                waitForLock(waitNode, sessionTimeout);//等待锁
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean tryLock() {
        if (isEntered(lockName)) {
            return true;
        }
        try {
            String splitStr = "_lock_";
            //创建临时子节点
            myZNode = zk.create(root + "/" + lockName + splitStr, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
            System.out.println(myZNode + " is created ");
            //取出所有子节点
            List<String> subNodes = zk.getChildren(root, false);
            //取出所有lockName的锁
            List<String> lockObjNodes = new ArrayList<>();
            for (String node : subNodes) {
                String _node = node.split(splitStr)[0];
                if(_node.equals(lockName)){
                    lockObjNodes.add(node);
                }
            }
            Collections.sort(lockObjNodes);
            System.out.println(myZNode + "==" + lockObjNodes.get(0));
            if(myZNode.equals(root+"/"+lockObjNodes.get(0))) {
                enter(lockName);
                //如果是最小的节点,则表示取得锁
                return true;
            }
            //如果不是最小的节点，找到比自己小1的节点
            String subMyZNode = myZNode.substring(myZNode.lastIndexOf("/") + 1);
            waitNode = lockObjNodes.get(Collections.binarySearch(lockObjNodes, subMyZNode) - 1);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean tryLock(long time, TimeUnit unit) {
        try {
            if(this.tryLock()) {
                return true;
            }
            return waitForLock(waitNode, time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean waitForLock(String lower, long waitTime) throws InterruptedException, KeeperException {
        Stat stat = zk.exists(root + "/" + lower,true);
        //判断比自己小一个数的节点是否存在,如果不存在则无需等待锁,同时注册监听
        if(stat != null) {
            System.out.println("Thread " + Thread.currentThread().getId() + " waiting for " + root + "/" + lower);
            this.latch = new CountDownLatch(1);
            this.latch.await(waitTime, TimeUnit.MILLISECONDS);
            this.latch = null;
        }
        return true;
    }

    public void unlock() {
        try {
            exit(lockName);
            System.out.println("unlock " + myZNode);
            zk.delete(myZNode,-1);
            myZNode = null;
            zk.close();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public void lockInterruptibly() {
        this.lock();
    }

    public Condition newCondition() {
        // pass
        return null;
    }

    private static final long DEFAULT_TIMEOUT = 30000;
}