/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.JSLBot.Packets.Types;

import java.nio.ByteBuffer;

/**
 *
 * @author Iain
 */
public abstract class Type {
    public Type() {}
    public Type(ByteBuffer in) { 
        this.read(in);
    }
    public abstract int size();
    public abstract void read(ByteBuffer in);
    public abstract void write(ByteBuffer out);
    public abstract String dump();
    public String toString() { return dump(); }
}
