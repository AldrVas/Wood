//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
import java.util.*;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.awt.*;
import java.io.*;
//import javax.imageio.*;
import javax.swing.*;

import org.jfree.svg.SVGGraphics2D;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;

public class Wood {
    static long MaxFileSize = 3000;
    static private DocumentBuilderFactory dbf;
    static private DocumentBuilder        db ;
    static private Document               doc;

    public static void main(String[] args) {
        // HostAddress=192.168.8.2 Login=mitrov Password=YourPassword PathIn=/in/ PathOut=/out/ WorkDir=c:\temp\5\ Mode=FTP ClearSource=False
        // PathIn=c:\temp\4\ PathOut=c:\temp\5\ Mode=Directory ClearSource=False
        ArrayList<File> TaskList;
        ArrayList<File> FilesToUpload;
        FilesToUpload = new ArrayList<File>();

        ArgumentsSet Args = new ArgumentsSet(args);

        String HostAddress = Args.GetValue("HostAddress");
        String Login = Args.GetValue("Login");
        String Password = Args.GetValue("Password");
        String PathIn = Args.GetValue("PathIn");
        String PathOut = Args.GetValue("PathOut");
        String WorkDir = Args.GetValue("workDir");
        String Mode = Args.GetValue("Mode");
        String ClearSourceStr = Args.GetValue("ClearSource");
        boolean ClearSource = ClearSourceStr.equalsIgnoreCase("true");

        Args.Print();

        //JohnsonTrotterTest();
        if (Mode.equalsIgnoreCase("directory")) {
            File dir = new File(PathIn);
            if(!dir.isDirectory()){
                System.out.println(PathIn + " is not a directory!");
                return;
            }
            for (File t : dir.listFiles()) {
                if (!t.isFile()){
                    continue;
                }
                Task MTask = new Task(0);
                MTask.LoadFromXML(t.getAbsolutePath());
                String XMLFileName = GetFileName(t.getName()) + "_res.xml";
                CutValue MyValue = MTask.Value1(5);
                if (MyValue != null) {
                    String SVGFileName = GetFileName(t.getName()) + ".svg";
                    MyValue.SaveToSVG(PathOut+"\\"+SVGFileName);
                    MyValue.SaveToXML(PathOut+"\\"+XMLFileName);
                } else {
                    System.out.println("No results");
                    SaveErrorXML(PathOut+"\\"+XMLFileName);
                }
                if (ClearSource) {
                    t.delete();
                }
            }
        }
        else if((Mode.equalsIgnoreCase("ftp"))){
            try {
                TaskList = ftpDownload(HostAddress, Login, Password, PathIn, WorkDir, ClearSource);
            } catch (FileNotFoundException ex) {
                System.out.println("FTP download error");
                return;
            }

            for (File t : TaskList) {
                Task MTask = new Task(0);
                MTask.LoadFromXML(t.getAbsolutePath());

                if (!t.delete()){
                    System.out.println(t.getAbsolutePath() + "  НЕ удален");
                }

                String XMLFileName = GetFileName(t.getPath()) + "_res.xml";
                CutValue MyValue = MTask.Value1(5);
                if (MyValue != null) {
                    String SVGFileName = GetFileName(t.getPath()) + ".svg";
                    MyValue.SaveToSVG(SVGFileName);
                    MyValue.SaveToXML(XMLFileName);
                    FilesToUpload.add(new File(SVGFileName));
                } else {
                    System.out.println("No results");
                    SaveErrorXML(XMLFileName);
                }
                FilesToUpload.add(new File(XMLFileName));
            }
            try {
                ftpUpload(HostAddress, Login, Password, FilesToUpload, PathOut);
            } catch (FileNotFoundException ex) {
                System.out.println("FTP upload error");
                //return;
            }
        }
        else{System.out.println("Unknown mode");}

        /*
        Task MTask = new Task(0);
        MTask.LoadFromXML("C:\\temp\\3\\task.xml");
        CutValue MyValue = MTask.Value1();
        if (MyValue != null){
            //MyValue.Draw();
            MyValue.SaveToSVG("C:\\temp\\3\\test.svg");
            MyValue.SaveToXML("C:\\temp\\3\\test.xml");
            //MyValue.Print();
        }else{
            System.out.println("No results");
        }
        */
    }


    private static ArrayList<File> ftpDownload(String hostAddress, String log, String password, String FromPath, String ToPath, boolean pClearSource) throws FileNotFoundException {
        ArrayList<File> Result = new ArrayList<File>();
        String extension;
        FTPClient fClient = new FTPClient();
        try {
            //System.out.println("Connecting...");
            fClient.connect(hostAddress);
            //System.out.println("Entering passive mode...");
            fClient.enterLocalPassiveMode();
            //System.out.println("Logging on...");
            fClient.login(log, password);

            //System.out.println("Initiate list parsing...");
            FTPListParseEngine engine = fClient.initiateListParsing(FromPath);

            while (engine.hasNext()) {
                //System.out.println("hasNext...");
                FTPFile[] files = engine.getNext(20);  // "page size" you want
                for (FTPFile f: files){
                    String CurFileName =  f.getName();
                    if (!f.isFile()){
                        continue;
                    }

                    if (f.getSize() > MaxFileSize){
                        System.out.println("File " + FromPath + CurFileName + " is too lage: " + f.getSize());
                        if (pClearSource){
                            fClient.deleteFile(FromPath + CurFileName);
                        }
                        continue;
                    }

                    extension = GetExtension(CurFileName);

                    //System.out.println(extension);
                    if (!extension.equals("xml")){
                        System.out.println("File " + FromPath + CurFileName + " has wrong extension: " + extension);
                        if (pClearSource){
                            fClient.deleteFile(FromPath + CurFileName);
                        }
                        continue;
                    }

                    //System.out.println(FromPath + CurFileName);
                    //System.out.println("Size:  " + Long.toString(f.getSize()));

                    FileOutputStream fOutput = new FileOutputStream(ToPath + CurFileName);

                    if (fClient.retrieveFile(FromPath + CurFileName, fOutput)) {
                        System.out.println("File downloaded " + FromPath + CurFileName);
                        Result.add(new File(ToPath + CurFileName));
                        if (pClearSource) {
                            fClient.deleteFile(FromPath + CurFileName);
                        }
                    }else{
                        System.out.println("File NOT downloaded " + FromPath + CurFileName);
                    }
                    fOutput.close();
                }
            }
            fClient.logout();
            fClient.disconnect();
        } catch (IOException ex) {
            System.err.println(ex);
        }
        return Result;
    }
    private static void ftpUpload(String hostAddress, String log, String password, ArrayList<File> pFilesToUpload, String ToPath) throws FileNotFoundException{
        FTPClient fClient = new FTPClient();
        try {
            //System.out.println("Connecting...");
            fClient.connect(hostAddress);
            //System.out.println("Entering passive mode...");
            fClient.enterLocalPassiveMode();
            //System.out.println("Logging on...");
            fClient.login(log, password);
            for (File f: pFilesToUpload){
                //System.out.println(f.getAbsolutePath());
                FileInputStream fInput = new FileInputStream(f.getAbsolutePath());
                String FileName = ToPath + f.getName();
                if (fClient.storeFile(FileName, fInput)){
                    System.out.println("File uploaded " + FileName);
                    fInput.close();
                    f.delete();
                }else{
                    System.out.println("File NOT uploaded " + FileName);
                }

            }
            fClient.logout();
            fClient.disconnect();
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
    private static String GetExtension(String FileName){
        String extension = "";
        int pos = FileName.lastIndexOf('.');
        if (pos > 0) {
            extension = FileName.substring(pos + 1);
        }
        return extension;
    }
    private static String GetFileName(String FileName){
        String name = FileName;
        int pos = FileName.lastIndexOf('.');
        if (pos > 1) {
            name = FileName.substring(0, pos);
        }
        return name;
    }
    private static boolean InitXML(){
        if (doc != null) {
            return true;
        }
        try{
            dbf = DocumentBuilderFactory.newInstance();
            db  = dbf.newDocumentBuilder();
            doc = db.newDocument();
            //System.out.println("XML Ok");
            return true;
        }catch (ParserConfigurationException e){
            System.out.println("XML Error");
            return false;
        }
    }
    private static void writeDocument(Document document, String path) throws TransformerFactoryConfigurationError {
        Transformer      trf;
        DOMSource        src;
        FileOutputStream fos;
        try {
            trf = TransformerFactory.newInstance().newTransformer();
            src = new DOMSource(document);
            fos = new FileOutputStream(path);

            StreamResult result = new StreamResult(fos);
            trf.transform(src, result);
        } catch (TransformerException e) {
            e.printStackTrace(System.out);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
    public static void SaveErrorXML(String FileName){
        if (!InitXML()) {
            return;
        }
        Element eCut = doc.createElement("Cut");
        doc.appendChild(eCut);
        eCut.setAttribute("Error", "No results");
        writeDocument(doc, FileName);
    }
}

class ArgumentsSet{
    static class Argument{
        public String Name, Value;
        private boolean Filled;
        Argument(String pName, String pValue){
            Name = pName;
            Value = pValue;
            Filled = true;
        }
        Argument(String S){
            int Index = S.indexOf("=");
            Filled = false;
            if ((Index <= 0) | (Index >= S.length())){
                System.out.println("Невереый параметр " + S);
                return;
            }
            Name = S.substring(0, Index);
            Value = S.substring(Index + 1);
            Filled = true;
        }
    }

    private final ArrayList<Argument> Arguments;
    public void AddArgument(Argument Arg){
        if (Arg == null){
            return;
        }
        if (!Arg.Filled){
            return;
        }
        if (Find(Arg.Name) == null) {
            Arguments.add(Arg);
        }
    }
    public void AddArgument(String Arg){
        Argument CurArg = new Argument(Arg);
        AddArgument(CurArg);
    }
    public void AddArgument(String pName, String pValue){
        Argument CurArg = new Argument(pName, pValue);
        AddArgument(CurArg);
    }
    public ArgumentsSet(String[] Args){
        Arguments = new ArrayList<Argument>();
        for(String CurArg: Args){
            AddArgument(CurArg);
        }
    }
    public Argument Find(String pName){
        for (Argument CurArg: Arguments){
            if (CurArg.Name.equalsIgnoreCase(pName)){
                return CurArg;
            }
        }
        return null;
    }
    public String GetValue(String pName){
        Argument CurArg = Find(pName);
        if (CurArg == null){
            return "";
        }
        return CurArg.Value;
    }
    public void Print(){
        for (Argument CurArg: Arguments){
            System.out.println(CurArg.Name + "=" + CurArg.Value);
        }
    }
}

class Sampling{
    private final int N;
    private final int k;
    private final int[] a;
    public ArrayList<int[]> Result;
    private void p2(int pos, int maxUsed) {
        if(pos == k) {
            //System.out.println(Arrays.toString(a));
            Result.add(Arrays.copyOf(a, a.length));
        } else {
            for(int i = maxUsed; i <= N; i++) {
                a[pos] = i;
                p2(pos+1,i);
            }
        }
    }
    private void SetResult(){
        p2(0, 0);
    }
    Sampling(int pN, int pk){
        N = pN;
        k = pk;
        a = new int[k];
        Result = new ArrayList<int[]>();
        SetResult();
    }
}

class ImageFrame extends JFrame {
    public ImageFrame(DetailsSet pDetails, IncisionsSet pIncisions ,Material pMaterial)
    {
        //pDetails.Print();
        setTitle("ImageTest");
        //setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setSize(pMaterial.Width + 100, pMaterial.Length + 100);

        // Добавление компонента к фрейму.

        ImageComponent component = new ImageComponent(pDetails, pIncisions, pMaterial);
        add(component);
    }
    //public static final int DEFAULT_WIDTH = 300;
    //public static final int DEFAULT_HEIGHT = 200;
}

class ImageComponent extends JComponent {
    DetailsSet Details;
    IncisionsSet Incisions;
    Material Material;
    public ImageComponent(DetailsSet pDetails, IncisionsSet pIncisions, Material pMaterial)
    {
        Material = pMaterial;
        Details = pDetails;
        Incisions = pIncisions;
        //pDetails.Print();
    }
    public void paintComponent(Graphics g) {
        // Отображение рисунка в левом верхнем углу.
        g.drawRect(0, 0, Material.Width, Material.Length);
        for (Incision i: Incisions.Incisions){
            g.drawRect(i.AbsoluteCoordinates.X, i.AbsoluteCoordinates.Y, i.Width, i.Length);
        }
        g.setColor(Color.blue);
        for (Detail i: Details.Details){
            //g.drawRect(i.AbsoluteCoordinates.X, i.AbsoluteCoordinates.Y, i.Width, i.Length);
            g.fillRect(i.AbsoluteCoordinates.X, i.AbsoluteCoordinates.Y, i.Width, i.Length);
        }

    }
}

enum Direction{
    Horizontal,
    Vertical
}

enum SortMethod{
    bySquare,
    byMaxLength,
    byCombine
}

class CutValue{
    int StartMPosX = 20, StartMPosY = 60, FontSize = 24, MaxPosX = 1500;
    static Point FramePosition;
    boolean Complete = false;
    int Value;
    DetailsSet Details;
    MaterialsSet Materials;
    IncisionsSet Incisions;
    int IncisionThickness;
    private DocumentBuilderFactory dbf;
    private DocumentBuilder        db ;
    private Document               doc;

    CutValue(int pIncisionThickness){
        IncisionThickness = pIncisionThickness;
        Details = new DetailsSet();
        Materials = new MaterialsSet();
        FramePosition = new Point(0, 0);
        Incisions = new IncisionsSet();
        Complete = false;
    }
    public DetailsSet GetDetails(Material ForMaterial){
        DetailsSet result = new DetailsSet();
        for (Detail i: Details.Details){
            if (i.OriginalMaterialID == ForMaterial.ID){
                result.Add(i);
            }
        }
        return result;
    }
    public IncisionsSet GetIncisions(Material ForMaterial) {
        IncisionsSet result = new IncisionsSet();
        for (Incision i : Incisions.Incisions) {
            if (i.OriginalMaterialID == ForMaterial.ID) {
                result.Add(i);
            }
        }
        return result;
    }

    public int NumberOfMaterials(){
        if (Materials == null){return 2147483647;}
        else if (Materials.GetSize() == 0){return 2147483647;}
        else {return Materials.GetSize();}

    }
    public boolean Compare(CutValue Other){ // true если this лучше, чем other
        if (Other == null){
            return true;
        }
        if (this.Materials.GetNotOffCutSquare() < Other.Materials.GetNotOffCutSquare()){
            return true;
        }else if(this.Materials.GetNotOffCutSquare() > Other.Materials.GetNotOffCutSquare()){
            return false;
        }else{
            return this.Value > Other.Value;
        }
    }

    public void Print(){
        System.out.print("---Value = ");
        System.out.println(Value);
        System.out.println("---Details: ");
        Details.Print();
        System.out.println("---Materials: ");
        Materials.Print();
        System.out.println("---Incisions: ");
        Incisions.Print();
    }
    public void Draw(){
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                for (Material i: Materials.Materials) {
                    DetailsSet CurDetails;
                    IncisionsSet CurIncisions;
                    CurDetails = GetDetails(i);
                    CurIncisions = GetIncisions(i);


                    ImageFrame frame = new ImageFrame(CurDetails, CurIncisions,  i);
                    frame.setLocation(FramePosition.X, FramePosition.Y);
                    FramePosition.X = FramePosition.X + i.Width;
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.setVisible(true);
                }
            }
        });
    }
    private Point GetSVGSize(){
        int MPosX = StartMPosX, MPosY = StartMPosY, CurRowHeight = 0, MaxRowWidth = 0, CurRowWidth = 0;
        for (Material m: Materials.Materials) {
            CurRowWidth = MPosX + m.Width + StartMPosX;
            if (CurRowWidth > MaxRowWidth){MaxRowWidth = CurRowWidth;}
            if (CurRowWidth > MaxPosX){
                MPosX = StartMPosX;
                MPosY += CurRowHeight + StartMPosY;
                CurRowHeight = 0;
            }
            if (m.Length > CurRowHeight){
                CurRowHeight = m.Length;}
            MPosX += m.Width + 20;
        }
        MPosY += CurRowHeight + StartMPosY;
        return(new Point(MaxRowWidth, MPosY));
    }
    public void SaveToSVG(String FileName){
        int MPosX = StartMPosX, MPosY = StartMPosY, CurRowHeight = 0;
        Point MaxSize;
        MaxSize = GetSVGSize();

        //System.out.println(MaxSize.X);
        //System.out.println(MaxSize.Y);
        SVGGraphics2D SVG = new SVGGraphics2D(MaxSize.X, MaxSize.Y);
        //SVGGraphics2D SVG = new SVGGraphics2D(200, 200);

        DetailsSet CurDetails;
        IncisionsSet CurIncisions;

        SVG.setFont(new Font("Dialog", Font.BOLD, FontSize));

        for (Material m: Materials.Materials) {

            if (MPosX + m.Width + StartMPosX > MaxPosX){
                MPosX = StartMPosX;
                MPosY += CurRowHeight + StartMPosY;
                CurRowHeight = 0;
            }
            if (m.Length > CurRowHeight){
                CurRowHeight = m.Length;}


            SVG.setColor(Color.black);
            SVG.drawString(m.ToStr(true), 3 + MPosX, MPosY - FontSize);
            SVG.drawRect(MPosX, MPosY, m.Width, m.Length);

            CurDetails = GetDetails(m);
            CurIncisions = GetIncisions(m);

            for (Incision i: CurIncisions.Incisions){
                SVG.drawRect(MPosX + i.AbsoluteCoordinates.X, MPosY + i.AbsoluteCoordinates.Y, i.Width, i.Length);
            }
            for (Detail d: CurDetails.Details){
                SVG.setColor(Color.gray);
                SVG.drawRect(MPosX + d.AbsoluteCoordinates.X,  MPosY + d.AbsoluteCoordinates.Y, d.Width, d.Length);
                SVG.setColor(Color.black);
                SVG.drawString(d.ToStr(true), 3 + MPosX + d.AbsoluteCoordinates.X, FontSize + MPosY + d.AbsoluteCoordinates.Y);
            }

            MPosX += m.Width + 20;
        }

        String fileContent = SVG.getSVGDocument();
        try (FileWriter writer = new FileWriter(FileName)){
            writer.write(fileContent);
        }catch (Exception e) {
            //return;
        }
    }
    private void writeDocument(Document document, String path) throws TransformerFactoryConfigurationError {
        Transformer      trf;
        DOMSource        src;
        FileOutputStream fos;
        try {
            trf = TransformerFactory.newInstance().newTransformer();
            src = new DOMSource(document);
            fos = new FileOutputStream(path);

            StreamResult result = new StreamResult(fos);
            trf.transform(src, result);
            fos.close();
        } catch (TransformerException e) {
            e.printStackTrace(System.out);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }
    private String DirectionToStr(Direction pDirection){
        if (pDirection == Direction.Vertical) {
            return "Vertical";
        }else{
            return "Horizontal";
        }
    }

    public boolean InitXML(){
        if (doc != null) {
            return true;
        }
        try{
            dbf = DocumentBuilderFactory.newInstance();
            db  = dbf.newDocumentBuilder();
            doc = db.newDocument();
            //System.out.println("XML Ok");
            return true;
        }catch (ParserConfigurationException e){
            System.out.println("XML Error");
            return false;
        }
    }
    public void SaveToXML(String FileName){
        if (!InitXML()) {
            return;
        }
        Element eCut = doc.createElement("Cut");
        doc.appendChild(eCut);
        eCut.setAttribute("IncisionThickness", Integer.toString(IncisionThickness));
        Element eMaterials = doc.createElement("Materials");

        for (Material m: Materials.Materials) {
            Element eMaterial = doc.createElement("Material");
            eMaterial.setAttribute("ID", Integer.toString(m.OriginalMaterialID));
            eMaterial.setAttribute("Length", Integer.toString(m.Length));
            eMaterial.setAttribute("Width", Integer.toString(m.Width));
            eMaterial.setAttribute("Offcut", Boolean.toString(m.Offcut));
            Element eIncisions = doc.createElement("Incisions");
            IncisionsSet CurIncisions = GetIncisions(m);
            for (Incision i: CurIncisions.Incisions){
                Element eIncision = doc.createElement("Incision");
                eIncision.setAttribute("X", Integer.toString(i.AbsoluteCoordinates.X));
                eIncision.setAttribute("Y", Integer.toString(i.AbsoluteCoordinates.Y));
                eIncision.setAttribute("Length", Integer.toString(i.Length));
                eIncision.setAttribute("Width", Integer.toString(i.Width));
                eIncision.setAttribute("Direction", DirectionToStr(i.Direction));
                eIncisions.appendChild(eIncision);
            }
            eMaterial.appendChild(eIncisions);

            Element eDetails = doc.createElement("Details");
            DetailsSet CurDetails = GetDetails(m);
            for (Detail d: CurDetails.Details){
                Element eDetail = doc.createElement("Detail");
                eDetail.setAttribute("X", Integer.toString(d.AbsoluteCoordinates.X));
                eDetail.setAttribute("Y", Integer.toString(d.AbsoluteCoordinates.Y));
                eDetail.setAttribute("Length", Integer.toString(d.Length));
                eDetail.setAttribute("Width", Integer.toString(d.Width));
                eDetails.appendChild(eDetail);
            }
            eMaterial.appendChild(eDetails);

            eMaterials.appendChild(eMaterial);
        }
        eCut.appendChild(eMaterials);
        writeDocument(doc, FileName);
    }
}

class PlacementMethod{
    boolean Rotated; //сдетали поворот детали перед разрезом
    boolean FirstCutType; // первый разрез горизонтальный (уменьшает length материала-родителя)
    PlacementMethod(boolean pRotated, boolean pFirstCutType){
        Rotated = pRotated;
        FirstCutType = pFirstCutType;
    }
    PlacementMethod(PlacementMethod pPlacement){
        Rotated = pPlacement.Rotated;
        FirstCutType = pPlacement.FirstCutType;
    }
}

class IncisionMethod{
    int MaterialIndex; int DetailIndex;
    PlacementMethod Placement;
    IncisionMethod(int pMaterialIndex, int pDetailIndex, PlacementMethod pPlacement){
        MaterialIndex = pMaterialIndex;
        DetailIndex = pDetailIndex;
        Placement = new PlacementMethod(pPlacement);
    }
    IncisionMethod(int pMaterialIndex, int pDetailIndex, boolean pRotated, boolean pFirstCutType){
        MaterialIndex = pMaterialIndex;
        DetailIndex = pDetailIndex;
        Placement = new PlacementMethod(pRotated, pFirstCutType);
    }
    IncisionMethod(IncisionMethod pIncisionMethod){
        MaterialIndex = pIncisionMethod.MaterialIndex;
        DetailIndex = pIncisionMethod.DetailIndex;
        Placement = new PlacementMethod(pIncisionMethod.Placement);
    }
}

class Point{
    int X, Y;
    Point(int pX, int pY){
        X = pX;
        Y = pY;
    }
    Point(Point Sample){
        X = Sample.X;
        Y = Sample.Y;
    }
}

class Incision{
    Point AbsoluteCoordinates;
    int Length, Width;
    int OriginalMaterialID;
    int IncisionThickness;
    Direction Direction;
    String ToStr(){
        return("Incision{" + OriginalMaterialID+"}{"+AbsoluteCoordinates.X+", "+AbsoluteCoordinates.Y+"}");
    }
    Incision(int pOriginalMaterialID, int pLength, int pWidth, Point pAbsoluteCoordinates, Direction pDirection){
        OriginalMaterialID = pOriginalMaterialID;
        AbsoluteCoordinates = new Point(pAbsoluteCoordinates);
        Length = pLength;
        Width = pWidth;
        Direction = pDirection;
    }
    Incision(Incision pIncision){
        OriginalMaterialID = pIncision.OriginalMaterialID;
        AbsoluteCoordinates = new Point(pIncision.AbsoluteCoordinates);
        Length = pIncision.Length;
        Width = pIncision.Width;
        Direction = pIncision.Direction;
    }
    public Element ToElement(Document doc){
        Element CurIncision  = doc.createElement("Incision");
        CurIncision.setAttribute("OriginalMaterialID", Integer.toString(OriginalMaterialID));
        CurIncision.setAttribute("X", Integer.toString(AbsoluteCoordinates.X));
        CurIncision.setAttribute("Y", Integer.toString(AbsoluteCoordinates.Y));
        CurIncision.setAttribute("Length", Integer.toString(Length));
        CurIncision.setAttribute("Width", Integer.toString(Width));
        return CurIncision;
    }

}

class IncisionsSet{
    public ArrayList<Incision> Incisions;
    public MaterialsSet UsedMaterial;

    public void Print(){
        for (Incision i:Incisions) {
            System.out.println(i.ToStr());
        }
    }

    public void Add(int pOriginalMaterialID,  int pLength, int pWidth ,Point pAbsoluteCoordinates, Direction pDirection){
        Incision NewIncision = new Incision(pOriginalMaterialID,  pLength, pWidth ,pAbsoluteCoordinates, pDirection);
        Incisions.add(NewIncision);
    }

    public void Add(Incision pIncision){
        Incision NewIncision = new Incision(pIncision);
        Incisions.add(NewIncision);
    }

    public IncisionsSet(){
        Incisions = new ArrayList<Incision>();
        UsedMaterial = new MaterialsSet();
    }

    public IncisionsSet(IncisionsSet Sample){
        Incisions = new ArrayList<Incision>();
        UsedMaterial = new MaterialsSet(Sample.UsedMaterial);
        for (Incision i: Sample.Incisions) {
            Incision CurIncisions = new Incision(i);
            Add(CurIncisions);
        }
    }

    public Element ToElement(Document doc){
        Element CurIncision;
        Element IncisionsSet = doc.createElement("IncisionsSet");
        for (Incision i:Incisions) {
            CurIncision = i.ToElement(doc);
            IncisionsSet.appendChild(CurIncision);
        }
        return IncisionsSet;
    }
}

class Material{
    int Length, Width, ID, OriginalMaterialID;
    boolean Offcut;
    Point AbsoluteCoordinates;
    Material(int pLength, int pWidth, boolean pOffcut, int pID){ // Новый оригинальный
        Length = pLength;
        Width = pWidth;
        ID = pID;
        OriginalMaterialID = pID;
        AbsoluteCoordinates = new Point(0, 0);
        Offcut = pOffcut;
    }

    Material(int pLength, int pWidth, Material Parent, Point RelativeCoordinates){ // Результат разреза
        Length = pLength;
        Width = pWidth;
        OriginalMaterialID = Parent.OriginalMaterialID;
        AbsoluteCoordinates = new Point(Parent.AbsoluteCoordinates.X + RelativeCoordinates.X, Parent.AbsoluteCoordinates.Y + RelativeCoordinates.Y);
        ID = -1;
        Offcut = true;
    }

    Material(Material Sample){ // Копия
        Length = Sample.Length;
        Width = Sample.Width;
        OriginalMaterialID = Sample.OriginalMaterialID;
        AbsoluteCoordinates = new Point(Sample.AbsoluteCoordinates);
        ID = Sample.ID;
        Offcut = Sample.Offcut;
    }

    public int Value(){
        return(GetSquare()^2);
    }
    public int GetSquare(){
        return(Length * Width);
    }
    public String ToStr(boolean pShort){
        if (!pShort){
            return("Material{" + Length + ", " + Width + "}{"+Offcut+"}{"+ID+"}{"+OriginalMaterialID+"}{"+AbsoluteCoordinates.X+", "+AbsoluteCoordinates.Y+"}");
        }else {
            return ("Material " + Length + " X " + Width);
        }
    }
    public Element ToElement(Document doc){
        Element CurMaterial  = doc.createElement("Material");
        CurMaterial.setAttribute("Length", Integer.toString(Length));
        CurMaterial.setAttribute("Width", Integer.toString(Width));
        CurMaterial.setAttribute("Offcut", Boolean.toString(Offcut));
        return CurMaterial;
    }
}

class Detail{
    int Length, Width, OriginalMaterialID;
    Point AbsoluteCoordinates;
    Detail(int pLength, int pWidth){
        Length = pLength;
        Width = pWidth;
        OriginalMaterialID = -1;
        AbsoluteCoordinates = new Point(-1, -1);
    }
    Detail(Detail Sample){
        Length = Sample.Length;
        Width = Sample.Width;
        OriginalMaterialID = Sample.OriginalMaterialID;
        AbsoluteCoordinates = new Point(Sample.AbsoluteCoordinates);
    }
    Detail(int pLength, int pWidth, boolean pRotated, Material Parent){
        if (pRotated){
            Length = pWidth;
            Width = pLength;
        }else{
            Length = pLength;
            Width = pWidth;
        }
        OriginalMaterialID = Parent.OriginalMaterialID;
        AbsoluteCoordinates = new Point(Parent.AbsoluteCoordinates.X, Parent.AbsoluteCoordinates.Y);
    }
    public int GetSquare(){
        return(Length * Width);
    }

    public void Rotate(){
        int T = Width;
        Width = Length;
        Length = T;
    }

    public String ToStr(boolean pShort){
        if (!pShort){
            return("Detail{" + Length + ", " + Width + "}{"+OriginalMaterialID+"}{"+AbsoluteCoordinates.X+", "+AbsoluteCoordinates.Y+"}");
        }else{
            return(Length + " X " + Width);
        }

    }
    public Element ToElement(Document doc){
        Element CurDetail  = doc.createElement("Detail");
        CurDetail.setAttribute("OriginalMaterialID", Integer.toString(OriginalMaterialID));
        CurDetail.setAttribute("X", Integer.toString(AbsoluteCoordinates.X));
        CurDetail.setAttribute("Y", Integer.toString(AbsoluteCoordinates.Y));
        CurDetail.setAttribute("Length", Integer.toString(Length));
        CurDetail.setAttribute("Width", Integer.toString(Width));
        return CurDetail;
    }
}

class SquareDetailComparator implements Comparator<Detail> {
    public int compare(Detail one, Detail other) {
        return one.GetSquare() - other.GetSquare();
    }
}

class MaxLengthDetailComparator implements Comparator<Detail> {
    public int compare(Detail one, Detail other) {
        return Math.max(one.Length, one.Width) - Math.max(other.Length, other.Width);
    }
}

class CombineDetailComparator implements Comparator<Detail> {
    public int compare(Detail one, Detail other) {

        float LengthValue = (float)Math.max(one.Length, one.Width) / (float)Math.max(other.Length, other.Width);
        float SquareValue = (float)one.GetSquare() / (float)other.GetSquare();
        float TotalValue = LengthValue * SquareValue;

        if (TotalValue > 1){
            return (1);
        }else if (TotalValue < 1){
            return (-1);
        }else{
            return (0);
        }
    }
}

class SquareMaterialComparator implements Comparator<Material> {
    public int compare(Material one, Material other) {
        return one.GetSquare() - other.GetSquare();
    }
}

class MaxLengthMaterialComparator implements Comparator<Material> {
    public int compare(Material one, Material other) {
        return Math.max(one.Length, one.Width) - Math.max(other.Length, other.Width);
    }
}

class CombineMaterialComparator implements Comparator<Material> {
    public int compare(Material one, Material other) {

        float LengthValue = (float)Math.max(one.Length, one.Width) / (float)Math.max(other.Length, other.Width);
        float SquareValue = (float)one.GetSquare() / (float)other.GetSquare();
        float TotalValue = LengthValue * SquareValue;

        if (TotalValue > 1){
            return (1);
        }else if (TotalValue < 1){
            return (-1);
        }else{
            return (0);
        }
    }
}

class MaterialsSet{
    public ArrayList<Material> Materials;

    private void SortBySquare(){
        SquareMaterialComparator CurSquareComparator = new SquareMaterialComparator();
        Materials.sort(CurSquareComparator);
    }
    private void SortByMaxLength(){
        MaxLengthMaterialComparator CurMaxLengthComparator = new MaxLengthMaterialComparator();
        Materials.sort(CurMaxLengthComparator);
    }
    private void SortByCombine(){
        CombineMaterialComparator CurCombineComparator = new CombineMaterialComparator();
        Materials.sort(CurCombineComparator);
    }
    public void Sort(SortMethod Method){
        switch(Method) {
            case bySquare:
                SortBySquare();
                break;
            case byMaxLength:
                SortByMaxLength();
                break;
            case byCombine:
                SortByCombine();
                break;
            default:
                SortBySquare();
                break;
        }
    }

    public int GetSquare(){
        int result = 0;
        for (Material i:Materials) {
            result += i.GetSquare();
        }
        return result;
    }

    public int GetMaxSize(){
        int result = 0;
        for (Material i:Materials) {
            int CurSize = Integer.max(i.Length, i.Width);
            if (result < CurSize){
                result = CurSize;
            }
        }
        return result;
    }

    public int GetMaxSquare(){
        int result = 0;
        for (Material i:Materials) {
            int CurSquare = i.GetSquare();
            if (result < CurSquare){
                result = CurSquare;
            }
        }
        return result;
    }

    public int GetOffCutSquare(){
        int result = 0;
        for (Material i:Materials) {
            if (i.Offcut){
                result += i.GetSquare();
            }
        }
        return result;
    }

    public int GetNotOffCutSquare(){
        int result = 0;
        for (Material i:Materials) {
            if (!i.Offcut){
                result += i.GetSquare();
            }
        }
        return result;
    }

    public void Print(){
        for (Material i:Materials) {
            System.out.println(i.ToStr(false));
        }
    }

    public int Value() {
        int res = 0;
        for (Material i:Materials) {
            res += i.Value();
        }
        return res;
    }

    public void Add(int pLength, int pWidth, boolean pOffcut){
        Material NewMaterial = new Material(pLength, pWidth, pOffcut, Materials.size());
        Materials.add(NewMaterial);
    }

    public void Add(int pLength, int pWidth, Material Parent, Point RelativeCoordinates){
        Material NewMaterial = new Material(pLength, pWidth, Parent, RelativeCoordinates);
        Materials.add(NewMaterial);
    }

    public void Add(Material Sample){
        //Material NewMaterial = new Material(Sample.Length, Sample.Width, Sample.ID);
        Material NewMaterial = new Material(Sample);
        Materials.add(NewMaterial);
    }

    public void Delete(int Index){
        if (Index < Materials.size()){
            Materials.remove(Index);
        }
    }

    public Material GetMaterial(int Index){
        return Materials.get(Index);
    }

    public Material GetMaterialByID(int pID){
        if (pID < 0){
            return null;
        }
        for (Material i:Materials) {
            if (i.ID == pID)
                return i;
        }
        return null;
    }

    public int GetSize(){
        return  (Materials.size());
    }

    public boolean CheckIndex(int Index){
        return  (Index >= 0 && Index < Materials.size());
    }

    MaterialsSet(){
        Materials = new ArrayList<Material>();
    }

    MaterialsSet(MaterialsSet Sample){
        Materials = new ArrayList<Material>();
        for (Material i: Sample.Materials) {
            Material CurMaterial = new Material(i);
            Add(CurMaterial);
        }
    }
    public Element ToElement(Document doc){
        Element CurMaterial;
        Element MaterialsSet  = doc.createElement("MaterialsSet");
        for (Material i:Materials) {
            CurMaterial = i.ToElement(doc);
            MaterialsSet.appendChild (CurMaterial);
        }
        return MaterialsSet;
    }
}

class DetailsSet{
    public ArrayList<Detail> Details;
    public void Reverse(){
        Collections.reverse(Details);
    }
    private void SortBySquare(){
        SquareDetailComparator CurSquareComparator = new SquareDetailComparator();
        Details.sort(CurSquareComparator);
    }
    private void SortByMaxLength(){
        MaxLengthDetailComparator CurMaxLengthComparator = new MaxLengthDetailComparator();
        Details.sort(CurMaxLengthComparator);
    }
    private void SortByCombine(){
        CombineDetailComparator CurCombineComparator = new CombineDetailComparator();
        Details.sort(CurCombineComparator);
    }
    public void Sort(SortMethod Method){
        switch(Method) {
            case bySquare:
                SortBySquare();
                break;
            case byMaxLength:
                SortByMaxLength();
                break;
            case byCombine:
                SortByCombine();
                break;
            default:
                SortBySquare();
                break;
        }
    }

    public void Add(int pLength, int pWidth, boolean pRotated, Material Parent){
        Detail NewDetail = new Detail(pLength, pWidth, pRotated, Parent);
        Details.add(NewDetail);
    }

    public void Add(int pLength, int pWidth){
        Detail NewDetail = new Detail(pLength, pWidth);
        Details.add(NewDetail);
    }
    public void Add(Detail Sample){
        Detail NewDetail = new Detail(Sample);
        Details.add(NewDetail);
    }
    public void Delete(int Index){
        if (Index < Details.size()){
            Details.remove(Index);
        }
    }
    public void Print(){
        for (Detail i:Details) {
            System.out.println(i.ToStr(false));
        }
    }

    public void Rotate(int Index){
        Detail CurDetail = Details.get(Index);
        CurDetail.Rotate();
    }

    public Detail GetDetail(int Index){
        return Details.get(Index);
    }

    public int GetSize(){
        return Details.size();
    }

    public int GetSquare(){
        int result = 0;
        for (Detail i:Details) {
            result += i.GetSquare();
        }
        return result;
    }

    public int GetMaxSize(){
        int result = 0;
        for (Detail i:Details) {
            int CurSize = Integer.max(i.Length, i.Width);
            if (result < CurSize){
                result = CurSize;
            }
        }
        return result;
    }

    public int GetMaxSquare(){
        int result = 0;
        for (Detail i:Details) {
            int CurSquare = i.GetSquare();
            if (result < CurSquare){
                result = CurSquare;
            }
        }
        return result;
    }

    public boolean CheckIndex(int Index){
        return  (Index >= 0 && Index < Details.size());
    }

    DetailsSet(){
        Details = new ArrayList<Detail>();
    }

    DetailsSet(DetailsSet Sample){
        Details = new ArrayList<Detail>();
        for (Detail i: Sample.Details) {
            Add(i);
        }
    }
    public Element ToElement(Document doc){
        Element CurDetail;
        Element DetailsSet = doc.createElement("DetailsSet");
        for (Detail i:Details) {
            CurDetail = i.ToElement(doc);
            DetailsSet.appendChild(CurDetail);
        }
        return DetailsSet;
    }
}

class Task{
    MaterialsSet Materials;
    MaterialsSet BasicMaterials;
    DetailsSet Details;
    DetailsSet DetailsDone;
    MaterialsSet OriginalMaterialsUsed;
    IncisionsSet Incisions;
    private DocumentBuilderFactory dbf;
    private DocumentBuilder        db ;
    private Document               doc;
    int IncisionThickness;
    int Level;
    static int MaxResults = 1000;
    //static int MaxLocalResults = 15;
    static int ResultsCount = 0;

    //IncisionMethod MethodOfObtain;
    private void writeDocument(Document document, String path) throws TransformerFactoryConfigurationError {
        Transformer      trf;
        DOMSource        src;
        FileOutputStream fos;
        try {
            trf = TransformerFactory.newInstance().newTransformer();
            src = new DOMSource(document);
            fos = new FileOutputStream(path);

            StreamResult result = new StreamResult(fos);
            trf.transform(src, result);
        } catch (TransformerException e) {
            e.printStackTrace(System.out);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public void SaveToXML(String FileName){
        if (!CheckXML()) {
            return;
        }
        Element Task = doc.createElement("Task");
        doc.appendChild(Task);
        Task.setAttribute("IncisionThickness", Integer.toString(IncisionThickness));
        Element eIncisions = Incisions.ToElement(doc);
        Element eMaterials = Materials.ToElement(doc);
        Element eDetails = Details.ToElement(doc);
        Task.appendChild (eMaterials);
        Task.appendChild (eDetails);
        Task.appendChild (eIncisions);
        writeDocument(doc, FileName);
    }

    public void Print(){
        System.out.print("--Task Level ");
        System.out.println(Level);
        System.out.println("----Materials");
        Materials.Print();
        System.out.println("----Details");
        Details.Print();
        System.out.println("----DetailsDone");
        DetailsDone.Print();
    }

    public void LoadFromXML(String fileName){
        Clear();
        DefaultHandler handler = new DefaultHandler() {
            boolean tagOn = false; // флаг начала разбора тега
            // Метод вызывается, когда SAXParser начинает обработку тэга
            @Override
            public void startElement(String uri,
                                     String localName,
                                     String qName,
                                     Attributes attributes)
                    throws SAXException {
                String LengthStr, WidthStr;
                int L, W;
                if (qName.equalsIgnoreCase("Material")) {
                    LengthStr = attributes.getValue("Length");
                    WidthStr = attributes.getValue("Width");
                    try {
                        L = Integer.parseInt(LengthStr.trim());
                        W = Integer.parseInt(WidthStr.trim());
                        Materials.Add(L, W, true);
                    }
                    catch (NumberFormatException nfe) {
                        //System.out.println("NumberFormatException: " + nfe.getMessage());
                    }
                    //System.out.println(attributes.getValue("Length") + ", " + attributes.getValue("Width"));
                } else if (qName.equalsIgnoreCase("BasicMaterial")) {
                    LengthStr = attributes.getValue("Length");
                    WidthStr = attributes.getValue("Width");
                    try {
                        L = Integer.parseInt(LengthStr.trim());
                        W = Integer.parseInt(WidthStr.trim());
                        BasicMaterials.Add(L, W, false);
                    }
                    catch (NumberFormatException nfe) {
                        //System.out.println("NumberFormatException: " + nfe.getMessage());
                    }
                    //System.out.println(attributes.getValue("Length") + ", " + attributes.getValue("Width"));
                } else if (qName.equalsIgnoreCase("Detail")) {
                    LengthStr = attributes.getValue("Length");
                    WidthStr = attributes.getValue("Width");
                    try {
                        L = Integer.parseInt(LengthStr.trim());
                        W = Integer.parseInt(WidthStr.trim());
                        Details.Add(L, W);
                    }
                    catch (NumberFormatException nfe) {
                        //System.out.println("NumberFormatException: " + nfe.getMessage());
                    }
                    //System.out.println(attributes.getValue("Length") + ", " + attributes.getValue("Width"));
                }else if (qName.equalsIgnoreCase("Task")){
                    String IncisionThicknessStr = attributes.getValue("IncisionThickness");
                    try {
                        IncisionThickness = Integer.parseInt(IncisionThicknessStr.trim());
                    }
                    catch (NumberFormatException nfe) {
                        //System.out.println("NumberFormatException: " + nfe.getMessage());
                    }
                }
            }
            // Метод вызывается, когда SAXParser считывает текст между тэгами
            @Override
            public void characters(char ch[],
                                   int start, int length)
                    throws SAXException {
                // Проверка флага
                if (tagOn) {
                    // Флаг установлен
                    //System.out.println(new String(ch,start,length));
                    tagOn = false;
                }
            }
            @Override
            public void endElement(String uri,
                                   String localName,
                                   String qName)
                    throws SAXException
            {
                super.endElement(uri, localName, qName);
            }

            @Override
            public void startDocument() throws SAXException
            {
                //System.out.println("Начало разбора документа!");
            }

            @Override
            public void endDocument() throws SAXException
            {
                //System.out.println("Разбор документа завершен!");
            }
        };
        try {
            SAXParserFactory factory;
            factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            // Стартуем разбор XML-документа
            saxParser.parse(fileName, handler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Detail GetDetail(int Index){
        return Details.GetDetail(Index);
    }

    public Material GetMaterial(int Index){
        return Materials.GetMaterial(Index);
    }

    public Material GetBasicMaterial(int Index){
        return BasicMaterials.GetMaterial(Index);
    }

    public void AddMaterial(int pLength, int pWidth, boolean pOffcut){
        Materials.Add(pLength, pWidth, pOffcut);
    }

    public void AddMaterial(int pLength, int pWidth, Material Parent, Point RelativeCoordinates){
        Materials.Add(pLength, pWidth, Parent, RelativeCoordinates);
    }

    public void AddMaterial(Material Sample){
        Materials.Add(Sample);
    }

    public void AddFromBasic(int Index){
        Material CurBasicMaterial = GetBasicMaterial(Index);
        CurBasicMaterial.ID = Materials.GetSize();
        CurBasicMaterial.OriginalMaterialID = Materials.GetSize();
        Materials.Add(CurBasicMaterial);
    }

    public void DeleteDetail(int Index){
        if (Details.CheckIndex(Index)) { // CheckDetailIndex(Index)
            Details.Delete(Index);
        }
    }

    public void DeleteMaterial(int Index){
        if (Materials.CheckIndex(Index)) { // CheckMaterialIndex(Index)
            Materials.Delete(Index);
        }
    }

    public void AddDetail(int pLength, int pWidth){
        Details.Add(pLength, pWidth);
    }

    public void AddDetail(Detail Sample){
        Details.Add(Sample);
    }

    public void AddDetailDone(int pLength, int pWidth, boolean pRotated, Material Parent){
        //System.out.println("AddDetailDone "+ Integer.toString(Parent.OriginalMaterialID));
        DetailsDone.Add(pLength, pWidth, pRotated, Parent);
    }

    public void AddIncision(int pOriginalMaterialID, int pLength, int pWidth, Point pAbsoluteCoordinates, Direction pDirection) {
        Incisions.Add(pOriginalMaterialID, pLength, pWidth, pAbsoluteCoordinates, pDirection);
    }

    public void AddIncision(Incision pIncision) {
        Incisions.Add(pIncision);
    }

    public void AddMaterialUsed(Material Sample){
        OriginalMaterialsUsed.Add(Sample);
    }

    public boolean CheckDetailsIndex(int Index){
        return Details.CheckIndex(Index);
    }

    public boolean CheckMaterialIndex(int Index){
        return Materials.CheckIndex(Index);
    }

    public void Sort(){
        Materials.Sort(SortMethod.bySquare);
        Details.Sort(SortMethod.bySquare);
        Details.Reverse();
    }

    public Task Cut(IncisionMethod pIncisionMethod) {
        Material CurMaterial;
        int NewLength1, NewLength2, NewWidth1, NewWidth2;
        int NewX1, NewX2, NewY1, NewY2;
        Detail CurDetail;

        int DetailIndex = pIncisionMethod.DetailIndex;
        int MaterialIndex = pIncisionMethod.MaterialIndex;
        boolean Rotated = pIncisionMethod.Placement.Rotated;
        boolean FirstCutType = pIncisionMethod.Placement.FirstCutType;

        CurDetail = GetDetail(DetailIndex);
        CurMaterial = GetMaterial(MaterialIndex);

        if (Rotated & FirstCutType) {
            NewLength1 = CurMaterial.Length - CurDetail.Width;
            NewWidth1 = CurMaterial.Width;
            NewLength2 = CurDetail.Width;
            NewWidth2 = CurMaterial.Width - CurDetail.Length;
            NewX1 = 0;
            NewX2 = CurDetail.Length + IncisionThickness;
            NewY1 = CurDetail.Width + IncisionThickness;
            NewY2 = 0;
        }else if (Rotated & !FirstCutType) {
            NewLength1 = CurMaterial.Length - CurDetail.Width;
            NewWidth1 = CurDetail.Length;
            NewLength2 = CurMaterial.Length;
            NewWidth2 = CurMaterial.Width - CurDetail.Length;
            NewX1 = 0;
            NewX2 = CurDetail.Length + IncisionThickness;
            NewY1 = CurDetail.Width + IncisionThickness;
            NewY2 = 0;
        }else if (FirstCutType) { // if (!Rotated & FirstCutType)
            NewLength1 = CurMaterial.Length - CurDetail.Length;
            NewWidth1 = CurMaterial.Width;
            NewLength2 = CurDetail.Length;
            NewWidth2 = CurMaterial.Width - CurDetail.Width;
            NewX1 = 0;
            NewX2 = CurDetail.Width + IncisionThickness;
            NewY1 = CurDetail.Length + IncisionThickness;
            NewY2 = 0;
        }else{ // if (!Rotated & !FirstCutType)
            NewLength1 = CurMaterial.Length - CurDetail.Length;
            NewWidth1 = CurDetail.Width;
            NewLength2 = CurMaterial.Length;
            NewWidth2 = CurMaterial.Width - CurDetail.Width;
            NewX1 = 0;
            NewX2 = CurDetail.Width + IncisionThickness;
            NewY1 = CurDetail.Length + IncisionThickness;
            NewY2 = 0;
        }
        if (NewLength1 < 0 | NewWidth2 < 0 | NewWidth1 > CurMaterial.Width | NewLength2 > CurMaterial.Length){
            return null;
        }

        Point RelativeCoordinates1 = new Point(NewX1, NewY1);
        Point RelativeCoordinates2 = new Point(NewX2, NewY2);

        Point AbsolutCoordinates1 = new Point(CurMaterial.AbsoluteCoordinates.X, NewY1 - IncisionThickness + CurMaterial.AbsoluteCoordinates.Y);
        Point AbsolutCoordinates2 = new Point(NewX2 - IncisionThickness + CurMaterial.AbsoluteCoordinates.X, CurMaterial.AbsoluteCoordinates.Y);

        Incision Incision1 = new Incision(CurMaterial.OriginalMaterialID, IncisionThickness, NewWidth1, AbsolutCoordinates1, Direction.Horizontal);
        Incision Incision2 = new Incision(CurMaterial.OriginalMaterialID, NewLength2, IncisionThickness, AbsolutCoordinates2, Direction.Vertical);

        Task CutResult = new Task(this);
        NewLength1 -= IncisionThickness;
        NewWidth2 -=IncisionThickness;


        if (FirstCutType){
            if (NewLength1 > IncisionThickness & NewWidth1 > IncisionThickness) {
                CutResult.AddMaterial(NewLength1, NewWidth1, CurMaterial, RelativeCoordinates1);
                CutResult.AddIncision(Incision1);
            }
            if (NewLength2 > IncisionThickness & NewWidth2 > IncisionThickness) {
                CutResult.AddMaterial(NewLength2, NewWidth2, CurMaterial, RelativeCoordinates2);
                CutResult.AddIncision(Incision2);
            }
        }else{
            if (NewLength2 > IncisionThickness & NewWidth2 > IncisionThickness) {
                CutResult.AddMaterial(NewLength2, NewWidth2, CurMaterial, RelativeCoordinates2);
                CutResult.AddIncision(Incision2);
            }
            if (NewLength1 > IncisionThickness & NewWidth1 > IncisionThickness) {
                CutResult.AddMaterial(NewLength1, NewWidth1, CurMaterial, RelativeCoordinates1);
                CutResult.AddIncision(Incision1);
            }
        }
        CutResult.DeleteDetail(DetailIndex);
        CutResult.AddDetailDone(CurDetail.Length, CurDetail.Width, Rotated, CurMaterial);


        if (CurMaterial.ID >= 0){
            CutResult.AddMaterialUsed(CurMaterial);
        }
        //System.out.println("OriginalMaterialID = "+Integer.toString(CurMaterial.OriginalMaterialID));
        CutResult.DeleteMaterial(MaterialIndex);

        CutResult.Sort();
        return CutResult;
    }

    public CutValue Value1(int MaxBasicMaterials){
        Task Task1;
        CutValue CurBestValue, CurValue;
        CurBestValue = null; //?

        if (Integer.max(Materials.GetMaxSize(), BasicMaterials.GetMaxSize()) < Details.GetMaxSize() | Integer.max(Materials.GetMaxSquare(), BasicMaterials.GetMaxSquare()) < Details.GetMaxSquare()){
            return null;
        }

        for (int i = 0; i <= MaxBasicMaterials ; i++){ // i = Сколько BasicMaterials добавляем
            //System.out.println("Добавляем BasicMaterials: " + i);
            Sampling CurSampling = new Sampling(BasicMaterials.GetSize() - 1, i);
            for (int[] S: CurSampling.Result){
              //  System.out.println(Arrays.toString(S));
                Task1 = new Task(this);
                Task1.Level = 0;

                for (int j: S){
                    Task1.AddFromBasic(j);
                }
                Task1.ResultsCount = 0;
                CurValue = Task1.Value();


                if (CurValue == null) {
                    continue;
                }

                if (CurBestValue == null){
                    CurBestValue = CurValue;
                }else if (CurValue.Compare(CurBestValue)){
                    CurBestValue = CurValue;
                }
            }
            if (CurBestValue != null){
                return CurBestValue;
            }
        }
        return CurBestValue;
    }

    public CutValue Value(){
        CutValue CurBestResult;
        CurBestResult = null;
        if (Details.GetSize() == 0){
            CutValue Result = new CutValue(IncisionThickness);
            Result.Value = Materials.Value();
            Result.Details = DetailsDone;
            Result.Incisions = Incisions;
            Result.Materials = OriginalMaterialsUsed;
            Result.Complete = true;
            ResultsCount++;
            //System.out.println("ResultsCount = "+ResultsCount);
            return Result;
        }
        if (Materials.GetSquare() < Details.GetSquare() | Materials.GetMaxSize() < Details.GetMaxSize() | Materials.GetMaxSquare() < Details.GetMaxSquare()){
            ResultsCount++;
            //System.out.println("ResultsCount = "+ResultsCount);
            return null;
        }

        //CutValue CurBestResult = new CutValue(IncisionThickness);
        boolean b[] = {false, true};
        for (int j = 0; (j < Details.GetSize() ); j++){
            for (int i = 0; (i < Materials.GetSize() ); i++){
                for (boolean k: b){
                    for (boolean l: b){
                        //System.out.println(Integer.toString(ResultsCount));
                        if (ResultsCount > MaxResults){
                            //System.out.println("Достигнут глобальный максимум!");
                            return CurBestResult;
                        }
                        IncisionMethod CurIncisionMethod = new IncisionMethod(i, j, k, l);
                        Task CurCut = Cut(CurIncisionMethod);
                        if (CurCut != null) {
                            CutValue CurCutValue = CurCut.Value();
                            if (CurCutValue == null){
                                continue;
                            }
                            if (CurCutValue.Compare(CurBestResult)) {
                                CurBestResult = CurCutValue;
                            }
                        }
                    }
                }
            }
        }
        return CurBestResult;
    }

    public boolean CheckXML(){
        return(doc != null);
    }

    public void InitXML(){
        try{
            dbf = DocumentBuilderFactory.newInstance();
            db  = dbf.newDocumentBuilder();
            doc = db.newDocument();
            //System.out.println("XML Ok");
        }catch (ParserConfigurationException e){
            System.out.println("XML Error");
        }
    }

    public void Clear(){
        Level = 0;
        Materials = new MaterialsSet();
        Details = new DetailsSet();
    }

    Task(int pLevel){
        Materials = new MaterialsSet();
        Details = new DetailsSet();
        DetailsDone = new DetailsSet();
        OriginalMaterialsUsed = new MaterialsSet();
        Incisions = new IncisionsSet();
        BasicMaterials = new MaterialsSet();
        Level = pLevel;
        //ResultsCount = 0;
        InitXML();
    }

    Task(Task Sample){
        Materials = new MaterialsSet(Sample.Materials);
        BasicMaterials = new MaterialsSet(Sample.BasicMaterials);
        Details = new DetailsSet(Sample.Details);
        DetailsDone = new DetailsSet(Sample.DetailsDone);
        OriginalMaterialsUsed = new MaterialsSet(Sample.OriginalMaterialsUsed);
        IncisionThickness = Sample.IncisionThickness;
        Incisions = new IncisionsSet(Sample.Incisions);
        Level = Sample.Level + 1;
    }
}