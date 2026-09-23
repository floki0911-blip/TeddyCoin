package com.example.teddycoin

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val BG = Color(0xFF0D0917)
private val PANEL = Color(0xFF19132B)
private val PANEL2 = Color(0xFF241A3D)
private val GOLD = Color(0xFFFFC83D)
private val PURPLE = Color(0xFF8156FF)
private val CYAN = Color(0xFF42D8FF)
private val GREEN = Color(0xFF35D07F)
private val MUTED = Color(0xFFB9B1CB)

private data class BearStage(val name: String, val need: Long, val outfit: String, val accent: Color, val normal: Int, val blink: Int)

private fun stageFor(c: Long): BearStage = when {
    c >= 1_000_000L -> BearStage("Легендарный Мишка",1_000_000L,"Космический костюм • корона",Color(0xFF4FD8FF),R.drawable.teddy_4_normal,R.drawable.teddy_4_blink)
    c >= 100_000L -> BearStage("Золотой Мишка",100_000L,"Золотой костюм • корона",Color(0xFFFFD54F),R.drawable.teddy_3_normal,R.drawable.teddy_3_blink)
    c >= 10_000L -> BearStage("Стильный Мишка",10_000L,"Куртка • очки • цепь",Color(0xFFB56CFF),R.drawable.teddy_2_normal,R.drawable.teddy_2_blink)
    c >= 1_000L -> BearStage("Мишка в Худи",1_000L,"Худи • кепка • очки",Color(0xFF42C8FF),R.drawable.teddy_1_normal,R.drawable.teddy_1_blink)
    else -> BearStage("Обычный Мишка",0L,"Простая одежда",Color(0xFFFFA45B),R.drawable.teddy_0_normal,R.drawable.teddy_0_blink)
}
private fun next(c: Long) = when { c<1_000L->1_000L; c<10_000L->10_000L; c<100_000L->100_000L; c<1_000_000L->1_000_000L; else->1_000_000L }
private fun dayNow()=System.currentTimeMillis()/86_400_000L

class MainActivity:ComponentActivity(){
    override fun onCreate(b:Bundle?){super.onCreate(b);setContent{TeddyCoinApp(this)}}
}


private fun localUserId(p: android.content.SharedPreferences): String {
    val existing = p.getString("userId", null)
    if (existing != null) return existing
    val id = "tc_" + java.util.UUID.randomUUID().toString().replace("-", "")
    p.edit().putString("userId", id).apply()
    return id
}

@Composable
private fun TeddyCoinApp(context:Context){
    val p=remember{context.getSharedPreferences("teddy",Context.MODE_PRIVATE)}
    var coins by remember{mutableLongStateOf(p.getLong("coins",0))}
    var energy by remember{mutableIntStateOf(p.getInt("energy",500))}
    var maxEnergy by remember{mutableIntStateOf(p.getInt("maxEnergy",500))}
    var level by remember{mutableIntStateOf(p.getInt("level",1))}
    var taps by remember{mutableIntStateOf(p.getInt("taps",0))}
    var tab by remember{mutableIntStateOf(0)}
    var daily by remember{mutableLongStateOf(p.getLong("dailyClaim",-1))}
    var weekly by remember{mutableLongStateOf(p.getLong("weeklyClaim",-1))}
    var boost by remember{mutableLongStateOf(p.getLong("boostUntil",0))}
    var name by remember{mutableStateOf(p.getString("displayName","Игрок") ?: "Игрок")}
    var syncText by remember{mutableStateOf(if(SupabaseConfig.isConfigured) "☁️ Облако подключено" else "📱 Только устройство")}
    var leaderboard by remember{mutableStateOf<List<OnlineProfile>>(emptyList())}
    val scope=rememberCoroutineScope()
    val sync=remember{SupabaseSync()}
    val userId=remember{localUserId(p)}

    fun save(){
        p.edit().putLong("coins",coins).putInt("energy",energy).putInt("maxEnergy",maxEnergy)
            .putInt("level",level).putInt("taps",taps).putLong("dailyClaim",daily)
            .putLong("weeklyClaim",weekly).putLong("boostUntil",boost).putString("displayName",name).apply()
    }
    fun cloudSave(){
        if(!SupabaseConfig.isConfigured){syncText="📱 Только устройство";return}
        scope.launch{
            syncText="☁️ Синхронизация..."
            val result=sync.upsert(OnlineProfile(userId,name,coins,level,taps))
            syncText=if(result.isSuccess) "☁️ Сохранено в облако" else "⚠️ Ошибка облака"
            if(result.isFailure) Toast.makeText(context,"Не удалось сохранить в облако",Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(maxEnergy){while(true){delay(1500);if(energy<maxEnergy){energy++;save()}}}
    LaunchedEffect(tab){
        if(tab==4 && SupabaseConfig.isConfigured){
            sync.leaderboard().onSuccess{leaderboard=it}
        }
    }
    val today=dayNow()
    val boostOn=boost>System.currentTimeMillis()

    MaterialTheme(colorScheme=darkColorScheme(background=BG,surface=PANEL,primary=GOLD)){
        Surface(Modifier.fillMaxSize(),color=BG){
            when(tab){
                0->Home(coins,energy,maxEnergy,level,boostOn,daily!=today,weekly<0||today-weekly>=7,
                    onTap={if(energy>0){
                        coins+=if(boostOn)2 else 1;energy--;taps++
                        if(taps%100==0&&level<100)level++
                        save()
                    }},
                    onDaily={if(daily!=today){coins+=500;daily=today;save();cloudSave()}},
                    onWeekly={if(weekly<0||today-weekly>=7){coins+=5000;weekly=today;save();cloudSave()}},
                    onTab={tab=it},
                    onAccount={tab=5})
                1->Shop(coins,maxEnergy,boostOn,{tab=0},
                    {if(coins>=300&&energy<maxEnergy){coins-=300;energy=maxEnergy;save()}},
                    {if(coins>=3000&&!boostOn){coins-=3000;boost=System.currentTimeMillis()+300000;save()}},
                    {if(coins>=5000){coins-=5000;maxEnergy+=100;energy=maxEnergy;save()}})
                2->Tasks(coins,taps,{tab=0},{id->
                    val key="task_$id"
                    if(!p.getBoolean(key,false)){
                        coins+=when(id){1->100;2->500;3->2000;else->10000}
                        p.edit().putBoolean(key,true).apply();save();cloudSave()
                    }})
                3->Friends{tab=0}
                4->OnlineRating(leaderboard, syncText, {tab=0}, { 
                    if(SupabaseConfig.isConfigured) scope.launch{
                        sync.leaderboard().onSuccess{leaderboard=it}
                    }
                })
                5->Account(name,userId,syncText,{newName->
                    name=newName.trim().ifBlank{"Игрок"};save();cloudSave()
                },{save();cloudSave()},{tab=0})
                else->Home(coins,energy,maxEnergy,level,boostOn,daily!=today,weekly<0||today-weekly>=7,
                    { },{}, {},{tab=0},{tab=5})
            }
        }
    }
}

@Composable private fun Home(coins:Long,energy:Int,maxEnergy:Int,level:Int,boost:Boolean,daily:Boolean,weekly:Boolean,onTap:()->Unit,onDaily:()->Unit,onWeekly:()->Unit,onTab:(Int)->Unit,onAccount:()->Unit){
    val s=stageFor(coins);val n=next(coins);val prog=if(coins>=1_000_000)1f else (coins.toDouble()/n).coerceIn(0.0,1.0).toFloat()
    Column(Modifier.fillMaxSize().background(BG).padding(12.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("TeddyCoin",color=GOLD,fontSize=29.sp,fontWeight=FontWeight.ExtraBold);Text("Тапай • копи • открывай новые формы",color=MUTED,fontSize=11.sp)};IconButton({onAccount()}){Icon(Icons.Default.Settings,"Аккаунт",tint=Color.White)}}
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){Stat("🪙","$coins","коинов",Modifier.weight(1f));Stat("⚡","$energy/$maxEnergy","энергия",Modifier.weight(1f));Stat("⭐","$level","уровень",Modifier.weight(1f))}
        Spacer(Modifier.height(7.dp))
        Card(Modifier.fillMaxWidth(),RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=PANEL)){Column(Modifier.padding(11.dp)){Row{Text(s.name,color=Color.White,fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));if(boost)Text("⚡ x2",color=GREEN,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(5.dp));LinearProgressIndicator({prog},Modifier.fillMaxWidth().height(7.dp),color=s.accent,trackColor=Color(0xFF34294E));Text(if(coins>=1_000_000)"Все формы открыты 🏆" else "$coins / $n TC до следующей формы",color=MUTED,fontSize=10.sp)}}
        Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){AnimatedBear(s,onTap)}
        Text(if(boost)"⚡ x2 КОИНОВ ЗА ТАП" else "НАЖМИ НА МИШКУ",Modifier.fillMaxWidth(),color=if(boost)GREEN else Color.White,fontSize=16.sp,fontWeight=FontWeight.ExtraBold,textAlign=TextAlign.Center)
        Text("+1 TC за обычный тап",Modifier.fillMaxWidth(),color=MUTED,fontSize=10.sp,textAlign=TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Bonus("🎁","День\n+500",daily,onDaily,Modifier.weight(1f));Bonus("📅","Неделя\n+5K",weekly,onWeekly,Modifier.weight(1f));Bonus("🛍️","Магазин",true,{onTab(1)},Modifier.weight(1f));Bonus("📋","Задания",true,{onTab(2)},Modifier.weight(1f))}
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){Bottom("🏠","Главная",true){onTab(0)};Bottom("🛍️","Магазин"){onTab(1)};Bottom("📋","Задания"){onTab(2)};Bottom("👥","Друзья"){onTab(3)};Bottom("🏆","Рейтинг"){onTab(4)}}
    }
}

@Composable private fun AnimatedBear(s:BearStage,onTap:()->Unit){
    val jump=remember{Animatable(0f)};val scope=rememberCoroutineScope();var blink by remember{mutableStateOf(false)}
    LaunchedEffect(Unit){while(true){delay(Random.nextLong(3000,5001));blink=true;delay(140);blink=false}}
    androidx.compose.foundation.Image(painterResource(if(blink)s.blink else s.normal),contentDescription=s.name,modifier=Modifier.size(255.dp).offset(y=jump.value.dp).clickable{onTap();scope.launch{jump.animateTo(-13f,tween(90));jump.animateTo(0f,tween(140))}})
}

@Composable private fun Stat(i:String,v:String,l:String,m:Modifier)=Card(m.height(66.dp),RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=PANEL)){Column(Modifier.fillMaxSize().padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("$i $v",color=Color.White,fontSize=15.sp,fontWeight=FontWeight.Bold);Text(l,color=MUTED,fontSize=9.sp)}}
@Composable private fun Bonus(i:String,t:String,e:Boolean,click:()->Unit,m:Modifier)=Card(m.height(55.dp).clickable(e){click()},RoundedCornerShape(13.dp),colors=CardDefaults.cardColors(containerColor=if(e)PANEL2:Color(0xFF302A3C))){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(i,fontSize=17.sp);Text(t,color=if(e)Color.White else MUTED,fontSize=9.sp,textAlign=TextAlign.Center)}}
@Composable private fun Bottom(i:String,t:String,s:Boolean=false,click:()->Unit)=Card(Modifier.weight(1f).height(54.dp).clickable{click()},RoundedCornerShape(13.dp),colors=CardDefaults.cardColors(containerColor=if(s)Color(0xFF3A2760) else PANEL)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(i,fontSize=17.sp);Text(t,color=if(s)GOLD else MUTED,fontSize=8.sp)}}

@Composable private fun Shop(coins:Long,max:Int,boost:Boolean,back:()->Unit,a:()->Unit,b:()->Unit,c:()->Unit){
    Header("🛍️ Магазин",back);Column(Modifier.fillMaxSize().padding(16.dp)){Spacer(Modifier.height(52.dp));Text("Баланс: 🪙 $coins TC",color=GOLD,fontSize=18.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));Item("⚡","Полная энергия","300 TC",coins>=300,a);Item("🔥","x2 коинов на 5 минут","3 000 TC",coins>=3000&&!boost,b);Item("🔋","+100 к максимуму энергии","5 000 TC",coins>=5000,c)}
}
@Composable private fun Item(i:String,t:String,p:String,e:Boolean,click:()->Unit)=Card(Modifier.fillMaxWidth().padding(bottom=9.dp),RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=PANEL)){Row(Modifier.padding(13.dp),verticalAlignment=Alignment.CenterVertically){Text(i,fontSize=28.sp);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(t,color=Color.White,fontWeight=FontWeight.Bold);Text("Улучшение для игры",color=MUTED,fontSize=10.sp)};Button(click,enabled=e,colors=ButtonDefaults.buttonColors(containerColor=GOLD,contentColor=Color.Black)){Text(p,fontWeight=FontWeight.Bold)}}}
@Composable private fun Tasks(coins:Long,taps:Int,back:()->Unit,claim:(Int)->Unit){
    Header("📋 Задания",back);val ctx=androidx.compose.ui.platform.LocalContext.current;val p=remember{ctx.getSharedPreferences("teddy",Context.MODE_PRIVATE)}
    Column(Modifier.fillMaxSize().padding(16.dp)){Spacer(Modifier.height(52.dp));listOf(Triple(1,"Сделай 100 тапов",100),Triple(2,"Сделай 500 тапов",500),Triple(3,"Накопи 2 000 TC",2000),Triple(4,"Накопи 10 000 TC",10000)).forEach{(id,title,goal)->val done=p.getBoolean("task_$id",false);val value=if(id<3)taps else coins.toInt();val ready=value>=goal&&!done;val reward=when(id){1->100;2->500;3->2000;else->10000};Card(Modifier.fillMaxWidth().padding(bottom=9.dp),RoundedCornerShape(17.dp),colors=CardDefaults.cardColors(containerColor=PANEL)){Column(Modifier.padding(13.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,color=Color.White,fontWeight=FontWeight.Bold);Text("$value / $goal",color=MUTED,fontSize=10.sp)};Button({claim(id)},enabled=ready,colors=ButtonDefaults.buttonColors(containerColor=if(ready)GREEN else Color(0xFF40394B))){Text(if(done)"Получено" else "+$reward TC")}};Spacer(Modifier.height(6.dp));LinearProgressIndicator({(value.toFloat()/goal).coerceIn(0f,1f)},Modifier.fillMaxWidth().height(6.dp),color=if(done)GREEN else PURPLE,trackColor=Color(0xFF332A45))}}}}
}
@Composable private fun Friends(back:()->Unit){Header("👥 Друзья",back);Column(Modifier.fillMaxSize().padding(16.dp)){Spacer(Modifier.height(52.dp));Card(Modifier.fillMaxWidth(),RoundedCornerShape(19.dp),colors=CardDefaults.cardColors(containerColor=PANEL)){Column(Modifier.padding(18.dp)){Text("Приглашай друзей",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(7.dp));Text("Код приглашения и реферальные награды добавим следующим этапом.",color=MUTED);Spacer(Modifier.height(12.dp));Button({ },colors=ButtonDefaults.buttonColors(containerColor=GOLD,contentColor=Color.Black)){Text("Пригласить")}}}}}
@Composable private fun Rating(back:()->Unit){Header("🏆 Рейтинг",back);Column(Modifier.fillMaxSize().padding(16.dp)){Spacer(Modifier.height(52.dp));listOf("🥇 TeddyMaster" to "125 000 TC","🥈 CoinBear" to "98 500 TC","🥉 TeddyPro" to "76 200 TC","🧸 Ты" to "Продолжай!").forEach{(a,b)->Card(Modifier.fillMaxWidth().padding(bottom=9.dp),RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=PANEL)){Row(Modifier.padding(15.dp)){Text(a,color=Color.White,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Text(b,color=GOLD)}}}}}
@Composable private fun Header(t:String,back:()->Unit){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Default.ArrowBack,"Назад",tint=Color.White)};Text(t,color=Color.White,fontSize=23.sp,fontWeight=FontWeight.Bold)}}

@Composable
private fun Account(name:String,userId:String,status:String,saveName:(String)->Unit,saveCloud:()->Unit,back:()->Unit){
    Header("👤 Аккаунт",back)
    var value by remember(name){mutableStateOf(name)}
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Spacer(Modifier.height(52.dp))
        Card(Modifier.fillMaxWidth(),RoundedCornerShape(19.dp),colors=CardDefaults.cardColors(containerColor=PANEL)){
            Column(Modifier.padding(18.dp)){
                Text("Профиль TeddyCoin",color=Color.White,fontSize=21.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value,{value=it},label={Text("Имя игрока")},singleLine=true,modifier=Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("ID: $userId",color=MUTED,fontSize=10.sp)
                Spacer(Modifier.height(8.dp))
                Text(status,color=if(status.contains("ошиб",true)) Color(0xFFFF7D7D) else GREEN,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button({saveName(value)},colors=ButtonDefaults.buttonColors(containerColor=GOLD,contentColor=Color.Black)){Text("Сохранить имя")}
                    OutlinedButton({saveCloud()}){Text("Синхронизировать")}
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Облако сохраняет профиль и рейтинг. Для реального онлайна один раз укажи URL и anon/publishable key Supabase в SupabaseConfig.kt.",color=MUTED,fontSize=12.sp)
    }
}

@Composable
private fun OnlineRating(rows:List<OnlineProfile>,status:String,back:()->Unit,refresh:()->Unit){
    Header("🏆 Онлайн-рейтинг",back)
    Column(Modifier.fillMaxSize().padding(16.dp)){
        Spacer(Modifier.height(52.dp))
        Row(verticalAlignment=Alignment.CenterVertically){
            Text(status,color=if(status.contains("ошиб",true))Color(0xFFFF7D7D) else GREEN,fontSize=11.sp,modifier=Modifier.weight(1f))
            Button({refresh()},enabled=SupabaseConfig.isConfigured,colors=ButtonDefaults.buttonColors(containerColor=GOLD,contentColor=Color.Black)){Text("Обновить")}
        }
        Spacer(Modifier.height(10.dp))
        if(rows.isEmpty()){
            Card(Modifier.fillMaxWidth(),RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=PANEL)){
                Text(if(SupabaseConfig.isConfigured)"Пока нет игроков или рейтинг ещё не загрузился." else "Онлайн-рейтинг не подключён. Открой SUPABASE_SETUP.sql и вставь данные проекта в SupabaseConfig.kt.",color=MUTED,modifier=Modifier.padding(18.dp))
            }
        }else{
            rows.forEachIndexed{index,row->
                Card(Modifier.fillMaxWidth().padding(bottom=8.dp),RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=PANEL)){
                    Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                        Text("${index+1}",color=GOLD,fontWeight=FontWeight.Bold,modifier=Modifier.width(30.dp))
                        Column(Modifier.weight(1f)){Text(row.displayName,color=Color.White,fontWeight=FontWeight.Bold);Text("Ур. ${row.level} • ${row.taps} тапов",color=MUTED,fontSize=10.sp)}
                        Text("${row.coins} TC",color=GOLD,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
    }
}
