Wood

Программа оптимального раскроя древесины для мебельного производства.
На вход принимает задания на раскрой в xml формате:
---------------------------------------------
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<Task IncisionThickness="5">
	<BasicMaterial Length="1000" Width="800"/>
	<BasicMaterial Length="1000" Width="700"/>
	<Material Length="1000" Width="500"/>
	<Material Length="700" Width="300"/>

	<Detail Length="500" Width="600"/>
	<Detail Length="1000" Width="300"/>
	<Detail Length="900" Width="400"/>
	<Detail Length="100" Width="300"/>
	<Detail Length="100" Width="400"/>
	<Detail Length="100" Width="400"/>
	<Detail Length="100" Width="200"/>
	<Detail Length="100" Width="500"/>
	<Detail Length="1000" Width="550"/>
</Task>
---------------------------------------------

<Task/> - само задание на раскрой. Параметр IncisionThickness - толщина разреза

<Material Length="1000" Width="500"/> - имеющийся в единственном экземпляре материал 
для раскроя размером Length х Width. Программа использует каждый такой экземпляр лишь единожды.  
Обычно это остатки после предыдущих раскроев.

<BasicMaterial Length="1000" Width="800"/> - имеющиеся "основные" материалы для раскроя размером Length х Width. 
Программа может использовать для раскроя любое их количество. Подразумевается, что на складе есть неограниченный 
запас данного сортамента. Не имеет смысла задавать различные BasicMaterial одинакового размера.

<Detail Length="500" Width="600"/> - необходимая деталь размером Length х Width.

Запуск осуществляется из командной строки:

Пример 1:
java -cp Wood.jar Wood PathIn=c:\temp\4\ PathOut=c:\temp\5\ Mode=Directory ClearSource=False

Пример 2:
java -cp Wood.jar Wood HostAddress=192.168.8.2 Login=UserName Password=YourPassword PathIn=/in/ PathOut=/out/ WorkDir=c:\temp\5\ Mode=FTP ClearSource=False

Mode определяет режим работы прогаммы. 
Если Mode=Directory, то задания должны размещаться по локально. Если  Mode=FTP, то на ftp ресурсе.

PathIn - путь к заданиям на раскрой в формате xml. 
Если Mode=Directory - это локальный каталог, если Mode=FTP, то каталог на ftp ресурсе.

PathOut - путь к результатам раскроя в формате xml и svg. 
Если Mode=Directory - это локальный каталог, если Mode=FTP, то каталог на ftp ресурсе.

ClearSource определяет необходимость удаления программой файлов с заданиями на раскрой после их выполнения 
(ClearSource=True удалять, иначе нет)

В случае если Mode=FTP необходимо задать параметры доступа к ftp (HostAddress, Login, Password) 
и локальный путь к рабочему каталогу WorkDir (для временных файлов).

После запуска программы, все файлы с заданиями, находящимися по адресу PathIn будут обработаны и для 
каждого задания по адресу PathOut будет создан результат в формте xml (к имени файла задания 
будет добавлено "_res") и дополнение в формате svg (с именем файла задания) для наглядного ознакомления 
с результатом.

Если в PathIn положить задания Test1.xml и Test2.xml, то в PathOut окажуться файлы 
Test1_res.xml, Test1.svg, Test2_res.xml и Test2.svg,

Результатом раскроя приведенного в начале примера будет следующий xml файл:

--------------------------------------------------------------------
<?xml version="1.0" encoding="utf-8" standalone="no"?>
<Cut IncisionThickness="5">
  <Materials>
    <Material ID="0" Length="1000" Offcut="true" Width="500">
      <Incisions>
        <Incision Direction="Horizontal" Length="5" Width="500" X="0" Y="600" />
        <Incision Direction="Horizontal" Length="5" Width="500" X="0" Y="705" />
        <Incision Direction="Vertical" Length="100" Width="5" X="400" Y="605" />
        <Incision Direction="Vertical" Length="290" Width="5" X="100" Y="710" />
        <Incision Direction="Horizontal" Length="5" Width="100" X="0" Y="910" />
      </Incisions>
      <Details>
        <Detail Length="600" Width="500" X="0" Y="0" />
        <Detail Length="100" Width="400" X="0" Y="605" />
        <Detail Length="200" Width="100" X="0" Y="710" />
      </Details>
    </Material>
    <Material ID="3" Length="1000" Offcut="false" Width="700">
      <Incisions>
        <Incision Direction="Vertical" Length="1000" Width="5" X="550" Y="0" />
        <Incision Direction="Vertical" Length="1000" Width="5" X="655" Y="0" />
        <Incision Direction="Horizontal" Length="5" Width="100" X="555" Y="500" />
        <Incision Direction="Horizontal" Length="5" Width="100" X="555" Y="905" />
      </Incisions>
      <Details>
        <Detail Length="1000" Width="550" X="0" Y="0" />
        <Detail Length="500" Width="100" X="555" Y="0" />
        <Detail Length="400" Width="100" X="555" Y="505" />
      </Details>
    </Material>
    <Material ID="2" Length="1000" Offcut="false" Width="800">
      <Incisions>
        <Incision Direction="Vertical" Length="1000" Width="5" X="400" Y="0" />
        <Incision Direction="Horizontal" Length="5" Width="400" X="0" Y="900" />
        <Incision Direction="Vertical" Length="1000" Width="5" X="705" Y="0" />
      </Incisions>
      <Details>
        <Detail Length="900" Width="400" X="0" Y="0" />
        <Detail Length="1000" Width="300" X="405" Y="0" />
      </Details>
    </Material>
    <Material ID="1" Length="700" Offcut="true" Width="300">
      <Incisions>
        <Incision Direction="Horizontal" Length="5" Width="300" X="0" Y="100" />
      </Incisions>
      <Details>
        <Detail Length="100" Width="300" X="0" Y="0" />
      </Details>
    </Material>
  </Materials>
</Cut>
--------------------------------------------------------------------

<Cut/> - непосредственно раскрой, т.е. результат работы.
IncisionThickness - толщина разреза, взятая из задания.

<Materials> - набор материалов использованный в раскрое (выбранный из материалов задания).
<Material ID="0" Length="1000" Offcut="true" Width="500"> - экземпляр материала размером 
Length х Width использованный в раскрое (тот, который был определен в соответствующих тегах задания), 
Offcut - признак "обрезка", т.е. материал взят из Material (Offcut=true) или BasicMaterial (Offcut=false) задания,  
ID - номер материала в раскрое.

<Incisions/> - набор разрезов, выполненных с данным материалом.
<Incision Direction="Horizontal" Length="5" Width="300" X="0" Y="100" /> - разрез в направлении Direction 
размером Length х Width, начатый в точке X, Y данного материала относительно его левого верхнего угла. 
Либо Length, либо Width в зависимости от Direction должны иметь
значение IncisionThickness (т.е. информация в параметре Direction избыточна).

</Details> - набор необходимых деталей, вырезанных из данного материала.
<Detail Length="100" Width="300" X="0" Y="0" /> - необходимая деталь размером Length х Width, вырезанная 
из данного материала. Верхний левый угол детали находится в точке X, Y данного материала относительно его 
левого верхнего угла.

Наглядно результат раскроя можно увидеть в файле svg.