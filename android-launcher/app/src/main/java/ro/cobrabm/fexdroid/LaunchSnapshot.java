package ro.cobrabm.fexdroid;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** One immutable choice set supplies both prepared patches and runtime flags. */
final class LaunchSnapshot extends ContextWrapper {
    private final SharedPreferences captured;
    LaunchSnapshot(Context context){super(context);captured=new Frozen(PortSettings.prefs(context).getAll());}
    @Override public SharedPreferences getSharedPreferences(String name,int mode){return name.equals("tlou")?captured:super.getSharedPreferences(name,mode);}
    private static final class Frozen implements SharedPreferences {
        private final Map<String,Object> values=new HashMap<>();
        Frozen(Map<String,?> source){source.forEach((k,v)->values.put(k,v instanceof Set<?>?java.util.Collections.unmodifiableSet(new java.util.HashSet<>((Set<?>)v)):v));}
        public Map<String,?> getAll(){return java.util.Collections.unmodifiableMap(values);}
        public boolean contains(String k){return values.containsKey(k);}
        public String getString(String k,String d){Object v=values.get(k);return v==null?d:(String)v;}
        @SuppressWarnings("unchecked") public Set<String> getStringSet(String k,Set<String>d){Object v=values.get(k);return v==null?d:java.util.Collections.unmodifiableSet((Set<String>)v);}
        public int getInt(String k,int d){Object v=values.get(k);return v==null?d:(Integer)v;}
        public long getLong(String k,long d){Object v=values.get(k);return v==null?d:(Long)v;}
        public float getFloat(String k,float d){Object v=values.get(k);return v==null?d:(Float)v;}
        public boolean getBoolean(String k,boolean d){Object v=values.get(k);return v==null?d:(Boolean)v;}
        public Editor edit(){throw new UnsupportedOperationException("Launch settings are frozen until the next game launch");}
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
    }
}
