package Model.utils;

import static java.lang.Math.*;
/**
 * Created by boyinzhang on 4/8/17.
 */
public class GemInfo {
    public int diamond;
    public int emerald;
    public int onyx;
    public int ruby;
    public int sapphire;


    public GemInfo(int numDiamond, int numEmerald,int numOnyx, int numRuby, int numSapphire){
        this.diamond = numDiamond;
        this.emerald = numEmerald;
        this.onyx = numOnyx;
        this.ruby = numRuby;
        this.sapphire = numSapphire;
    }

    public GemInfo(int numberPerGem){
        this(numberPerGem,numberPerGem,numberPerGem,numberPerGem,numberPerGem);
    }

    public int GemTotalNum(){
        return diamond+emerald+onyx+ruby+sapphire;
    }

    public int GemMaxTypeNum(){
        int maxNum = max(diamond,emerald);
        maxNum = max(maxNum,onyx);
        maxNum = max(maxNum,ruby);
        maxNum = max(maxNum,sapphire);
        return maxNum;
    }

    public void updateInfo(int deltaDiamond, int deltaEmerald,int deltaOnyx, int deltaRuby, int deltaSapphire){
        this.diamond += deltaDiamond;
        this.emerald += deltaEmerald;
        this.onyx += deltaOnyx;
        this.ruby += deltaRuby;
        this.sapphire += deltaSapphire;
    }

    public void setGems(int newDiamond, int newEmerald,int newOnyx, int newRuby, int newSapphire){
        this.diamond += newDiamond;
        this.emerald += newEmerald;
        this.onyx += newOnyx;
        this.ruby += newRuby;
        this.sapphire += newSapphire;
    }

    public static void combineGems(GemInfo currentGems, GemInfo addGems){
        currentGems.diamond += addGems.diamond;
        currentGems.emerald += addGems.emerald;
        currentGems.onyx += addGems.onyx;
        currentGems.ruby += addGems.ruby;
        currentGems.sapphire += addGems.sapphire;
    }

    public static void reduceGems(GemInfo currentGems, GemInfo addGems){
        currentGems.diamond -= addGems.diamond;
        currentGems.emerald -= addGems.emerald;
        currentGems.onyx -= addGems.onyx;
        currentGems.ruby -= addGems.ruby;
        currentGems.sapphire -= addGems.sapphire;
    }
}
