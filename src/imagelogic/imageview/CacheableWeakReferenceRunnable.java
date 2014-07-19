
package imagelogic.imageview;

import java.lang.ref.WeakReference;

abstract class CacheableWeakReferenceRunnable<T> implements Runnable {

    private final WeakReference<T> mObjectRef;

    public CacheableWeakReferenceRunnable(T object) {
        mObjectRef = new WeakReference<T>(object);
    }

    @Override
    public final void run() {
        T object = mObjectRef.get();

        if (null != object) {
            run(object);
        }
    }

    public abstract void run(T object);

}
