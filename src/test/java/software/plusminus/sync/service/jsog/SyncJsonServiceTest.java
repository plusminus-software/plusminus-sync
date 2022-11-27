package software.plusminus.sync.service.jsog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SyncJsonServiceTest {
    
    private SyncJsonService jsonService = new SyncJsonService();
    
    @Test
    public void withoutFiltering() {
        MyObject object = getObject();
        String json = jsonService.toJson(object, w -> true);
        assertEquals("{\"myString\":\"stringValue\",\"myInteger\":10,\"myBoolean\":true}", json);
    }

    @Test
    public void filteredByFieldName() {
        MyObject object = getObject();
        String json = jsonService.toJson(object, w -> !w.getName().equals("myString"));
        assertEquals("{\"myInteger\":10,\"myBoolean\":true}", json);
    }

    @Test
    public void filteredByAnnotation() {
        MyObject object = getObject();
        String json = jsonService.toJson(object, w -> w.getAnnotation(Deprecated.class) == null);
        assertEquals("{\"myString\":\"stringValue\",\"myInteger\":10}", json);
    }
    
    private MyObject getObject() {
        MyObject object = new MyObject();
        object.myString = "stringValue";
        object.myInteger = 10;
        object.myBoolean = true;
        return object;
    }
    
    private static class MyObject {
        private String myString;
        private int myInteger;
        @Deprecated
        private boolean myBoolean;

        public String getMyString() {
            return myString;
        }

        public void setMyString(String myString) {
            this.myString = myString;
        }

        public int getMyInteger() {
            return myInteger;
        }

        public void setMyInteger(int myInteger) {
            this.myInteger = myInteger;
        }

        public boolean isMyBoolean() {
            return myBoolean;
        }

        public void setMyBoolean(boolean myBoolean) {
            this.myBoolean = myBoolean;
        }
    }

}