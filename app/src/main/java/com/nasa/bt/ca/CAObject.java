package com.nasa.bt.ca;

public class CAObject {

    private CABasic caBasic;
    private String sign;

    public CAObject() {
    }

    public CAObject(CABasic caBasic, String sign) {
        this.caBasic = caBasic;
        this.sign = sign;
    }

    public CABasic getCaBasic() {
        return caBasic;
    }

    public void setCaBasic(CABasic caBasic) {
        this.caBasic = caBasic;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    @Override
    public String toString() {
        return "CAObject{" +
                "caBasic=" + caBasic +
                ", sign='" + sign + '\'' +
                '}';
    }
}
