import com.github.ob_yekt.simpleskills.fishing.*;
import com.github.ob_yekt.simpleskills.fishing.FishingFightModel.*;
import com.github.ob_yekt.simpleskills.fishing.FishSizeModel.*;
import java.util.*;
public class SizeTest {
  static int fails=0;
  static void check(boolean ok,String what){ if(!ok){fails++;} System.out.println((ok?"PASS ":"FAIL ")+what); }
  static double f(int lvl){ return Math.max(0,Math.min(1,(lvl-1)/98.0)); }
  static double[] ranks(double[] v){ Integer[] idx=new Integer[v.length]; for(int i=0;i<v.length;i++)idx[i]=i; Arrays.sort(idx,Comparator.comparingDouble(i->v[i])); double[] r=new double[v.length]; for(int i=0;i<v.length;i++) r[idx[i]]=i; return r; }
  static double pearson(double[] a,double[] b){ double ma=Arrays.stream(a).average().orElse(0),mb=Arrays.stream(b).average().orElse(0),c=0,va=0,vb=0; for(int i=0;i<a.length;i++){c+=(a[i]-ma)*(b[i]-mb);va+=Math.pow(a[i]-ma,2);vb+=Math.pow(b[i]-mb,2);} return c/Math.sqrt(va*vb); }
  public static void main(String[] a){
    Map<String,Profile> P=new LinkedHashMap<>();
    P.put("cod",new Profile(35,110,1.0e-5,3)); P.put("salmon",new Profile(45,110,1.1e-5,3));
    P.put("tropical",new Profile(6,25,2.5e-5,3)); P.put("pufferfish",new Profile(15,45,5.0e-5,3));
    Random r=new Random(42);
    for(var e:P.entrySet()){
      Profile p=e.getValue(); double minW=1e9,maxW=0,minL=1e9,maxL=0,sumW=0; int n=20000; int inversions=0;
      List<Result> rs=new ArrayList<>();
      for(int i=0;i<n;i++){ Result x=FishSizeModel.generate(p,r::nextDouble,r::nextGaussian); rs.add(x);
        minW=Math.min(minW,x.weightKg());maxW=Math.max(maxW,x.weightKg());minL=Math.min(minL,x.lengthCm());maxL=Math.max(maxL,x.lengthCm());sumW+=x.weightKg(); }
      // correlation length/weight
      double mL=rs.stream().mapToDouble(Result::lengthCm).average().orElse(0), mW=sumW/n, cov=0,vL=0,vW=0;
      for(Result x:rs){cov+=(x.lengthCm()-mL)*(x.weightKg()-mW);vL+=Math.pow(x.lengthCm()-mL,2);vW+=Math.pow(x.weightKg()-mW,2);}
      double[] rl=ranks(rs.stream().mapToDouble(Result::lengthCm).toArray()), rw=ranks(rs.stream().mapToDouble(Result::weightKg).toArray());
      double corr=pearson(rl,rw);
      System.out.printf("%-10s len %.1f-%.1f cm | weight %.2f-%.2f kg (avg %.2f, ref %.2f) | corr %.3f%n",e.getKey(),minL,maxL,minW,maxW,mW,p.referenceWeightKg(),corr);
      check(minL>=p.minLengthCm()-0.05 && maxL<=p.maxLengthCm()+0.05,e.getKey()+" length within range");
      check(corr>0.98,e.getKey()+" weight rank strongly tied to length rank (spearman>0.98)");
      // XP bounds
      int base=1250;
      int lo=FishSizeModel.catchXp(base,1.0,p,minW), hi=FishSizeModel.catchXp(base,1.0,p,maxW);
      check(lo>=(int)(base*0.6)-1 && hi<=base*3 && hi>lo, e.getKey()+" xp small="+lo+" big="+hi+" (limits "+(int)(base*0.6)+".."+base*3+")");
      double sLo=FishSizeModel.strengthMultiplier(p,minW), sHi=FishSizeModel.strengthMultiplier(p,maxW);
      check(sLo>=0.80&&sHi<=1.35&&sHi>sLo, e.getKey()+String.format(" strength %.2f..%.2f within 0.80..1.35",sLo,sHi));
      // monotonic: heavier never gives less xp / strength
      boolean mono=true; double pw=0; int px=0; double ps=0;
      for(double w=minW; w<=maxW; w+=(maxW-minW)/200.0){int x=FishSizeModel.catchXp(base,1.0,p,w);double s=FishSizeModel.strengthMultiplier(p,w); if(x<px||s<ps-1e-9) mono=false; px=x;ps=s;}
      check(mono,e.getKey()+" xp & strength never decrease with weight");
    }
    // Fight difficulty vs fish size (cod), attentive/average players, levels 1/50/99
    Profile cod=P.get("cod");
    System.out.println("\n== Fight: average player (lag 14 ticks, counter 0.7) by fish size ==");
    for(int lvl: new int[]{1,50,99}) for(double w: new double[]{0.4,2.2,6.0,10.5}){
      double sm=FishSizeModel.strengthMultiplier(cod,w);
      Params pr=new Params(1+f(lvl),1-0.40*f(lvl),1+f(lvl),1.0,sm,1.0/FishSizeModel.fatigueDivisor(sm),FishSizeModel.counterEffect(sm));
      int caught=0,n=3000; double ticks=0;
      for(int i=0;i<n;i++){
        Random rr=new Random(i*131L+lvl); FishingFightModel m=new FishingFightModel(pr,rr);
        ArrayDeque<Integer> dir=new ArrayDeque<>();ArrayDeque<Double> ten=new ArrayDeque<>(); int t=0; Outcome o=Outcome.ONGOING;
        while(o==Outcome.ONGOING&&t<2400){ dir.addLast(m.move().dir);ten.addLast(m.tensionRatio()); while(dir.size()>15){dir.removeFirst();ten.removeFirst();}
          double c=(dir.peekFirst()!=0&&dir.peekFirst()==m.move().dir)?0.7:0; boolean reel=ten.peekFirst()<0.65&&(!m.isPulling()||m.fatigue()>0.55); o=m.tick(reel,c);t++;}
        if(o==Outcome.CAUGHT){caught++;ticks+=t;}
      }
      System.out.printf("L%-3d fish %.1f kg (strength x%.2f): caught %5.1f%%  avg %.1fs%n",lvl,w,sm,100.0*caught/n,caught>0?ticks/caught/20:0);
    }
    // ---- Direction pipeline ----
    System.out.println("\n== Direction pipeline ==");
    check(FishingFightModel.counterFromOffset(-1,+20)>0.99,"fish LEFT + camera turned RIGHT (yaw +20) -> full counter");
    check(FishingFightModel.counterFromOffset(+1,-20)>0.99,"fish RIGHT + camera turned LEFT (yaw -20) -> full counter");
    check(FishingFightModel.counterFromOffset(-1,-20)==0.0,"fish LEFT + camera turned LEFT -> no counter");
    check(FishingFightModel.counterFromOffset(+1,+20)==0.0,"fish RIGHT + camera turned RIGHT -> no counter");
    check(FishingFightModel.counterFromOffset(0,+20)==0.0,"fish resting -> no counter");
    // Minecraft geometry: view vector of yaw y is (-sin y, cos y); right vector of a viewer is (-cos y, -sin y)
    for(double yaw: new double[]{0,45,90,135,180,-90,-135,33.3}){
      double y=Math.toRadians(yaw); double fx=-Math.sin(y), fz=Math.cos(y);
      double rx=-fz, rz=fx;                        // manager: right = (-nz, nx) with n = player->fish = viewer forward
      double expRx=-Math.cos(y), expRz=-Math.sin(y); // right-hand side of the viewer in Minecraft
      check(Math.abs(rx-expRx)<1e-9&&Math.abs(rz-expRz)<1e-9,String.format("manager 'right' vector == viewer's right at yaw %.1f",yaw));
      // turning the camera right (yaw+d) must rotate the view vector towards that right vector
      double d=Math.toRadians(5); double nfx=-Math.sin(y+d), nfz=Math.cos(y+d);
      check((nfx-fx)*rx+(nfz-fz)*rz>0,String.format("yaw+5 turns the view towards the right at yaw %.1f",yaw));
    }
    System.out.println("\nFAILS="+fails);
  }
}
