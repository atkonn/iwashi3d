//this file was used to generate all the c code to display the models.
//Some parameters need to be changed specifically for each obj file.

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.Arrays;

public class ObjToC {

//	strings which define parameters in generated c file
	static String path = "/home/konno/work/t/Atlantis";
	static String filename = "iwashi13.obj";

	static String header = "#include \"atlantis-internal.h\"";

	static String comments = "//points are of the form (y,z,x).\n"+
	"//model made using Blender and generated using script.";

	static String methodName = "Iwashi";
	
	static String openGLinit = "    glRotatef(180.0, 0.0, 1.0, 0.0);\n"+
	"    glRotatef(90.0, 0.0, 1.0, 0.0);\n"+
	"    glEnable(GL_CULL_FACE);";

	static String openGLfin  = "    glDisable(GL_CULL_FACE);";
							   //"    glEnable(GL_DEPTH_TEST);";

	static String glRenderSide = "GL_FRONT";
	
	static int maxNumZeros = 5;
	static int startFrame = 1;
	
	//scale: 2000f for coral.
	//scale: 4000f for fish/clownFish/bfish.
	//scale: 5000f for crabs.
	//scale: 12000f for chromis.
	static float scaleFactor = 12000f;
	static boolean animation = false;
	static boolean ignoreMaterials = false;
	static boolean copyDiffuseToAmbient = true;
	static boolean wire = false;
	static boolean setCullFace = true;
	static boolean separateObjects = true;
	static boolean labelSeparateObjects = true;
	
	static float ambientFactor = 0.8f;
	static float diffuseFactor = 0.8f;
	static float specularFactor = 0.1f;
	static float shininessFactor = 1.0f;
	
	
	
	static boolean wave = true;
	static boolean linear = true;

	//xOffset - 1.1f for fish and bfish
	//        - 0.6f for fish2
	//        - 0.4f for chromis
	
	static float xOffset = 0.4f;
	static float amplitude = 0.5f;

	//frequencyFactor - 0.2f for fish and bfish
	//				  - 0.25f for fish2 and chromis

	static float frequencyFactor = 0.25f;
	
	
	
//	other things
	static float[][][] pts;
	static float[][][] texture;
	static float[][][] normal;
	
	static ArrayList<int[]> indices;	//(pt, texture, normal) indices
	static int[] orderedIndices;		//the indices order from (f)ace ordering 
	
	static ArrayList<int[]> groups;		// (# sides, # polygons with same # sides)
	

	static ArrayList<boolean[]> materialAttributes;
	static ArrayList<String> materialNames;
	static ArrayList<String> objectNames;

	static int vCounter = 0, vtCounter = 0, vnCounter = 0, fCounter = 0;
	static int aniCounter = 0;
	static int fileCounter = 0;
	static String pre = "_";
	static String post = "";
	
	static boolean initDrawingRoutine = false;
	static boolean initAnimateRoutine = false;
	static boolean initVertexArray    = false;
	
	static String[] attributes = { "shininess", "ambient", "diffuse", "specular"};
	static String[][] attr;
	static String transparency = null;
	static String currentMaterial = "";
	static boolean newMaterial = false;
	static boolean disableColorMaterial = false;
	static boolean twoSided = false;
	static int prevSides = 0;
	static int sides = 0;
	

	public static void main(String[] args) throws IOException {

		int len = path.length();
		if (path.charAt(len-1)=='/') path = path.substring(0,len-1);

		len = filename.length();
		if (filename.substring(len-4,len).equalsIgnoreCase(".obj")) {
			filename = filename.substring(0,len-4);
		}
		
		materialNames = new ArrayList<String>();
		materialAttributes = new ArrayList<boolean[]>();
		attr= new String[4][4];
		
		objectNames = new ArrayList<String>();

		System.out.println(header);
		System.out.println ("\n"+comments+"\n");

		checkFiles();
		
		if (!ignoreMaterials) mtlFile();

		checkObjFile();
		pts    = new float[fileCounter][vCounter][3];
		texture= new float[fileCounter][vtCounter][2];
		normal = new float[fileCounter][vnCounter][3];
		indices = new ArrayList<int[]>();
		orderedIndices = new int[fCounter];

		groups = new ArrayList<int[]>();
		
		
		fillVerticesNormalsObjFile();
		fillIndicesObjFile();
		printVerticesNormalsIndicesObjFile();
		
		animateObjFile();
		vertexArrayObjFile();
		initEndObjFile();

	}

	static String addLeadingZeros(int x, int maxZeros) {
		String s = "";

		int j=1;
		for (int i=0; i<maxZeros; i++) {
			j*=10;
			if (x<j) s+="0";
		}
		return s+x;
	}
	
	static void checkFiles() throws IOException {
		fileCounter = startFrame;
		if (animation) {
			try {
				while (true) {
					new FileInputStream(getFullFilename(filename,fileCounter,"obj"));
					/*
							path+"/"+filename+"_"+
							addLeadingZeros(fileCounter, maxNumZeros)+".obj");
							*/
					fileCounter++;
				}
			}
			catch (IOException e) {
				fileCounter-=startFrame;
			}
		}
	}
	
	static void mtlFile() throws IOException {
		FileInputStream fis;
		BufferedReader br;

		try {
			fis = new FileInputStream(path+"/"+filename+".mtl");
			br = new BufferedReader(new InputStreamReader(fis));
		}
		catch (IOException e) {
			return;
		}

		newMaterial = false;
		transparency = "1.0000";

		for (int i=0; i<attr.length; i++) {
			for (int j=0; j<attr[0].length; j++) {
				attr[i][j]=null;
			}
		}

		String input;
		while ((input=br.readLine())!=null) {
			StringTokenizer st = new StringTokenizer(input, " \t");

			if (!st.hasMoreTokens()) continue;

			String s = st.nextToken(); 
			if (s.equals("newmtl")) {
				String mat = "";
				while (st.hasMoreTokens()) {
					mat += st.nextToken(" \t.,;:+-*/\\=!?\"'[](){}|");
				}
				if (mat.equals("")) continue;

				newAttributes();

				currentMaterial = mat;

				boolean[] b = new boolean[4];
				materialNames.add(mat);

				for (int i=0; i<b.length; i++) {
					b[i]=false;
				}
				materialAttributes.add(b);

				newMaterial = true;
			}
			else if (s.equals("Ns")) {
				if (!st.hasMoreTokens() || currentMaterial.equals("")) continue;

				attr[0][0]=st.nextToken();
			}
			else if (s.equals("Ka")) {
				if (st.countTokens()<3 || currentMaterial.equals("")) continue;

				attr[1][0]=st.nextToken();
				attr[1][1]=st.nextToken();
				attr[1][2]=st.nextToken();
				if (st.hasMoreTokens()) attr[1][3]=st.nextToken();
			}
			else if (s.equals("Kd")) {
				if (st.countTokens()<3 || currentMaterial.equals("")) continue;

				attr[2][0]=st.nextToken();
				attr[2][1]=st.nextToken();
				attr[2][2]=st.nextToken();
				if (st.hasMoreTokens()) attr[2][3]=st.nextToken();
			}
			else if (s.equals("Ks")) {
				if (st.countTokens()<3 || currentMaterial.equals("")) continue;
				attr[3][0]=st.nextToken();
				attr[3][1]=st.nextToken();
				attr[3][2]=st.nextToken();
				if (st.hasMoreTokens()) attr[3][3]=st.nextToken();
			}
			else if (s.equals("d") || s.equals("Tr")) {
				if (!st.hasMoreTokens() || currentMaterial.equals("")) continue;

				transparency = st.nextToken();
			}
		}
		newAttributes();
	}
	static void newAttributes() {
		if (!newMaterial) return;

		if (copyDiffuseToAmbient)
		{
			attr[1][0] = attr[2][0];
			attr[1][1] = attr[2][1];
			attr[1][2] = attr[2][2];
			attr[1][3] = attr[2][3];
		}

		attr[0][0] = ""+(Float.parseFloat(attr[0][0])*shininessFactor);
		for (int i=0; i<3; i++)
		{
			attr[1][i] = ""+(Float.parseFloat(attr[1][i])*ambientFactor);
			attr[2][i] = ""+(Float.parseFloat(attr[2][i])*diffuseFactor);
			attr[3][i] = ""+(Float.parseFloat(attr[3][i])*specularFactor);
		}
		
		Boolean userDefined = false;
		if (currentMaterial.substring(0,Math.min("userDefined".length(),currentMaterial.length())).equalsIgnoreCase("userDefined"))
			userDefined = true;
		
		
		for (int i=0; i<4; i++) {
			if (attr[i][0]!=null) 
				materialAttributes.get(materialAttributes.size()-1)[i]=true;

			System.out.print ("static "+(userDefined ? "": "const ")+"GLfloat "+currentMaterial+"_"+attributes[i]+"[] = {" +attr[i][0]);
			if (i!=0) {
				System.out.print (","+attr[i][1]+","+attr[i][2]+",");
				if (attr[i][3]==null) System.out.print(transparency);
				else System.out.print (attr[i][3]);
			}
			System.out.println ("};");
		}
		materialAttributes.get(materialAttributes.size()-1)[0]=true;

		for (int i=0; i<attr.length; i++) {
			for (int j=0; j<attr[0].length; j++) {
				attr[i][j]=null;
			}
		}
		System.out.println();
	}
	
	static String getFullFilename(String filename, int fc, String fileExtension) {
		return path+"/"+filename+(animation ? pre+addLeadingZeros(fc, maxNumZeros)+post : "")+"."+fileExtension;
	}
	
	static void checkObjFile () throws IOException {
		FileInputStream fis = new FileInputStream(path+"/"+filename+".obj");
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		vCounter = 0; vtCounter = 0; vnCounter = 0; fCounter = 0;
		
		String input;
		while ((input=br.readLine())!=null) {
			StringTokenizer st = new StringTokenizer(input);
			if (!st.hasMoreTokens()) continue;
			
			String s = st.nextToken(); 
			if (s.equalsIgnoreCase("v")) vCounter++;
			else if (s.equalsIgnoreCase("vt")) vtCounter++;
			else if (s.equalsIgnoreCase("vn")) vnCounter++;
			else if (s.equalsIgnoreCase("f")) {
			
				for (; st.hasMoreTokens(); fCounter++)
					st.nextToken();
			}
		}
		br.close();
		fis.close();
	}

	static void fillVerticesNormalsObjFile () throws IOException {

		for (int j=0; j<fileCounter; j++) {
			FileInputStream fis = new FileInputStream(getFullFilename(filename,j+1,"obj"));
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			vCounter = 0; vtCounter=0; vnCounter = 0;

			String input;
			while ((input=br.readLine())!=null) {
				StringTokenizer st = new StringTokenizer(input, " \t");

				if (!st.hasMoreTokens()) continue;
				String s = st.nextToken(); 
				if (s.equalsIgnoreCase("v")) {

					float [] rt = new float[3];
					for (int i=0; i<3; i++) {
						s = st.nextToken();
						rt[i] = Float.parseFloat(s)*scaleFactor;
					}

					pts[j][vCounter] = new float[] { rt[0], rt[1], rt[2] }; // y,z,x
					vCounter++;
					
				}
				if (s.equalsIgnoreCase("vt")) {

					float [] rt = new float[2];
					for (int i=0; i<2; i++) {
						s = st.nextToken();
						rt[i] = Float.parseFloat(s);
					}

					texture[j][vtCounter] = new float[] { rt[0], rt[1] };
					vtCounter++;
					
				}
				else if (s.equalsIgnoreCase("vn")) {
					float [] rt = new float[3];
					float sumSquares = 0;
					for (int i=0; i<3; i++) {
						s = st.nextToken();
						rt[i] = Float.parseFloat(s);
						sumSquares+=rt[i]*rt[i];
					}

					if (sumSquares==0) sumSquares = 1;

					for (int i=0; i<3; i++) {
						//rt[i]/=sumSquares;
					}

					normal[j][vnCounter] = new float[] { rt[0], rt[1], rt[2] }; // y,z,x
					vnCounter++;
				}

			}
			br.close();
			fis.close();
		}
	}
	static void fillIndicesObjFile () throws IOException {

		for (int j=0; j<fileCounter; j++) {
			FileInputStream fis = new FileInputStream(getFullFilename(filename,j+1,"obj"));
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			fCounter = 0;
			int distinctFCounter = 0;
			
			prevSides = 0;
			sides = 0;
			
			Boolean prevSpecifiedNormal = false;
			Boolean specifiedNormal = false;
			
			Boolean prevSpecifiedTexture = false;
			Boolean specifiedTexture = false;
			
			Boolean newObject = false;
			
			int objectCounter = -1;
			int materialIndex = -1;
			int prevMaterialIndex = -1;

			String input;
			while ((input=br.readLine())!=null) {
				StringTokenizer st = new StringTokenizer(input);

				if (!st.hasMoreTokens()) continue;
				String s = st.nextToken(); 
				if (s.equalsIgnoreCase("f")) {
					
					sides = 0;
					specifiedNormal = true;
					
					nextElement:
						
					while (st.hasMoreTokens())
					{

						int[] array = { -1, -1, -1 };

						StringTokenizer st2 = new StringTokenizer(st.nextToken(),"/",true);
						for (int i=0; i<3 && st2.hasMoreTokens(); i++)
						{
							s = st2.nextToken();

							if (s.equals("/")) continue;

							array[i] = Integer.parseInt(s)-1;

							if (st2.hasMoreTokens())
								s = st2.nextToken();

						}
						if (array[1]==-1) specifiedTexture = false;
						if (array[2]==-1) specifiedNormal  = false;
						
						for (int i=0; i<indices.size(); i++)
						{
							if (Arrays.equals(array,indices.get(i)))
							{
								orderedIndices[fCounter] = i;
								fCounter++; sides++;
								continue nextElement;
							}
						}
						indices.add(array);

						orderedIndices[fCounter] = distinctFCounter;
						fCounter++; distinctFCounter++; sides++;
					}
					
					//group indices by side, #, (object), (material)
					if (groups.size()==0 || prevSides!=sides ||
							prevSpecifiedNormal !=specifiedNormal ||
							prevSpecifiedTexture != specifiedTexture ||
							sides>4 ||
							(newObject && separateObjects) ||
							materialIndex!=prevMaterialIndex)
					{
						prevSides = sides;
						prevSpecifiedNormal = specifiedNormal;
						prevMaterialIndex = materialIndex;
						
						groups.add(new int[] { sides, 1, objectCounter, materialIndex });
					}
					else
					{
						groups.get(groups.size()-1)[1]+=1;
					}
					
					newObject = false;
				}
				else if (s.equalsIgnoreCase("o")) {
					newObject = true;
					objectCounter++;
					objectNames.add(st.nextToken());
				}
				else if (s.equalsIgnoreCase("usemtl")) {
					s = st.nextToken();
					for (int i=0; i<materialNames.size(); i++)
					{
						if (s.equals(materialNames.get(i)))
						{
							materialIndex = i;
							break;
						}
					}
				}
			}
			br.close();
			fis.close();
		}
	}

	
	static void printVerticesNormalsIndicesObjFile () {
		
		int size = indices.size();
		for (int k=0; k<3; k++)
		{
			if (k==1 && vtCounter==0) continue;
			if (k==2 && vnCounter==0 ) continue;
			
			
		System.out.print("static float "+methodName);
		if (k==0) System.out.println ("Points["+size*3+"] = {");
		else if (k==1) System.out.println("Texture["+size*2+"] = {");
		else if (k==2) System.out.println ("Normals["+size*3+"] = {");
		for (int i=0; i<size; i++)
		{
			System.out.print("    ");
			for (int j=0; j<(k==1 ? 2: 3); j++)
			{
				int ind = indices.get(i)[k];
				if (ind>=0) {
					if (k==0)      System.out.print(    pts[0][ind][j]);
					else if (k==1) System.out.print(texture[0][ind][j]);
					else if (k==2) System.out.print( normal[0][ind][j]);
				}
				else 
					System.out.print ("0");
				
				if (i<size-1 || j<(k==1 ? 1: 2))
					System.out.print (", ");
			}
			System.out.println ();
		}
		System.out.println ("};\n");
		}

		size = orderedIndices.length;
		System.out.println ("static unsigned int "+methodName+"Indices["+size+"] = {");

		int c = 0;
		for (int g=0; g<groups.size(); g++)
		{
			for (int i=0; i<groups.get(g)[1]; i++) {
				System.out.print("    ");

				for (int j=0; j<groups.get(g)[0]; j++, c++)
				{
					System.out.print (orderedIndices[c]);
					if (c < size-1 || j<groups.get(g)[0]-1)
						System.out.print(", ");
				}
				System.out.println ();
			}
		}
		System.out.println ("};\n");

	}

	static void animateObjFile () throws IOException {

		if (!initAnimateRoutine) {
			System.out.println("\nvoid\nAnimate"+methodName+"(float t)\n{");
			
			if (fileCounter>1)
				System.out.println("    t = fmodf(t, "+fileCounter+");");
			
			System.out.println("    int   ti = (int) t;");
			if (fileCounter>1) {
			System.out.println("    float dt = t-ti;");
			}
			if (wave) System.out.println("    float w = 2*PI*(t-ti);");
			
			if (fileCounter>1)
				System.out.println("\n    switch (ti) {");
			
			//System.out.println("int time = (int) fmodf(t,"+fileCounter+");");
			initAnimateRoutine = true;
		}

		
		for (int j=0; j<fileCounter; j++) {

			for (int c=0; c<indices.size(); c++) {


				int ptInd = indices.get(c)[0];
				int ptIndDup = -1;
				
				for (int c2=0; c2<c; c2++)
				{
					if (ptInd==indices.get(c2)[0])
					{
						ptIndDup = c2;
						break;
					}
				}
				
				
				if (c==0 && fileCounter>1) {
					if (j>0) System.out.println ("\tbreak;\n");
					System.out.println("    case("+j+") :");
				}


				for (int i=0; i<3; i++) {

					String pre = (fileCounter>1 ? "\t": "    ");
					pre+=methodName+"Points["+i+"+3*"+c+"] ="+(pts[j][ptInd][i]);

					int j2 = (j+1)%fileCounter; 
					
					if (ptIndDup<0) {
					if (linear) { //linear interpolation
						float A = (pts[j2][ptInd][i]-pts[j][ptInd][i]);
						if (A!=0)
							System.out.println (pre+"+dt*"+A+";");

					}
					else { //cubic spline interpolation 

						float B   = coefficientB (j, ptInd, i, pts);
						float Bp1 = coefficientB (j2, ptInd, i, pts);

						float A = coefficientA(B, Bp1, j, ptInd, i, pts);
						float C = coefficientC(B, Bp1);

						if (A!=0 && B!=0 && C!=0)
							System.out.println (pre+"+dt*"+A+"+dt*dt*"+B+"+dt*dt*dt*"+C+";");

					}
					}
					if (i==2) {
						if (wave && pts[j][ptInd][0]>=xOffset*scaleFactor) {

							if (ptIndDup<0)
							{
							float A = frequencyFactor*(pts[j][ptInd][0]/scaleFactor-xOffset);
							A = (float) (((double) A)%(2*Math.PI));
							if (A>0) A-=2*Math.PI;


							System.out.println(pre+"+"+(amplitude*(pts[j][ptInd][0]-xOffset*scaleFactor))+
									"*sinf(w"+(A>=0 ? "+" : "")+A+");");
							}
							else {
								System.out.print ((fileCounter>1 ? "\t": "    "));
								System.out.println (methodName+"Points["+i+"+3*"+c+"] = "+methodName+"Points["+i+"+3*"+ptIndDup+"];");
							}
						}
					}
				}


			}
		}
		if (initAnimateRoutine) {
			if (fileCounter>1)
				System.out.println ("\tbreak;\n    }");
			System.out.println ("}\n");
		}
		
	}

	static void vertexArrayObjFile () {

		
		if (wire) System.out.println("\nvoid\nDraw"+methodName+"(int wire)\n{");
		else  System.out.println("\nvoid\nDrawAnimated"+methodName+"()\n{");
		
		if (groups.size()== 0) return;
		
		int c = 0;
		Boolean prevUseNormals = false;
		Boolean useNormals = false;
		
		int materialIndex = -1;
		
		System.out.println ("    glPushAttrib(GL_CURRENT_BIT | GL_ENABLE_BIT | GL_LIGHTING_BIT);");
		
		for (int i=0; i<groups.size(); i++)
		{
			if (i!=0) System.out.println ();
			
			int numElements = groups.get(i)[0]*groups.get(i)[1];
			
			String type = "GL_POLYGON";
			if (groups.get(i)[0]==1)      type="GL_POINTS";
			else if (groups.get(i)[0]==2) type="GL_LINE_LOOP";
			else if (groups.get(i)[0]==3) type="GL_TRIANGLES";
			else if (groups.get(i)[0]==4) type="GL_QUADS";

			prevUseNormals = useNormals;
			useNormals = (indices.get(orderedIndices[c])[1]<0);
			
			if (prevUseNormals!=useNormals)
			{
				if (useNormals) System.out.println("    glEnableClientState(GL_NORMAL_ARRAY);");
				else System.out.println("    glDisableClientState (GL_NORMAL_ARRAY);");
			}
			
			if (i==0)
			{
				System.out.println("    glEnableClientState(GL_VERTEX_ARRAY);");
				System.out.println("    glVertexPointer (3, GL_FLOAT, 0, "+methodName+"Points);");
				System.out.println("    glNormalPointer (GL_FLOAT, 0, "+methodName+"Normals);\n");
			}

			int go = (i==0 ? -1 : groups.get(i-1)[2]);
			if (labelSeparateObjects && separateObjects &&
				groups.get(i)[2]!=go && groups.get(i)[2]>=0)
				System.out.println ("    //"+objectNames.get(groups.get(i)[2]));
			
			if (groups.get(i)[3]>=0)
			{
				if (materialIndex!=groups.get(i)[3])
				{
					materialIndex = groups.get(i)[3];
					printUseMaterial(materialIndex);
				}
			}
			else if (twoSided)
			{
				 System.out.println ("    glLightModelf (GL_LIGHT_MODEL_TWO_SIDE, 0.0f);");
				 if (setCullFace) System.out.println ("    glEnable(GL_CULL_FACE);");
				 twoSided = false;
			}
			
			if (wire) System.out.println ("    "+(i==0 ? "GLenum ": "")+"cap = wire ? GL_LINE_LOOP : "+type+";");

			System.out.println("    glDrawElements("+(wire ? "cap": type)+", "+(numElements)+", GL_UNSIGNED_INT, &("+methodName+"Indices["+(c)+"]));");
			
			c+= numElements;
		}
		
		if (useNormals) System.out.println("    glDisableClientState (GL_NORMAL_ARRAY);");
		
		System.out.println ("    glPopAttrib();");
		System.out.println("}\n");
	}

	static void printUseMaterial(int materialIndex)
	{
		if (!disableColorMaterial) System.out.println("    glDisable (GL_COLOR_MATERIAL);");
		disableColorMaterial = true;

		System.out.println("    glTexEnvi (GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);");

		String renderSide = glRenderSide;
		if (materialNames.get(materialIndex).substring(0,Math.min("userDefined_DS".length(), materialNames.get(materialIndex).length())).equalsIgnoreCase("userDefined_DS"))
		{
			renderSide = "GL_FRONT_AND_BACK";
			
			if (!twoSided) {
				System.out.println ("    glLightModelf (GL_LIGHT_MODEL_TWO_SIDE, 1.0f);");
				if (setCullFace) System.out.println ("    glDisable(GL_CULL_FACE);");
			}
			twoSided = true;
		}
		else if (twoSided)
		{
			 System.out.println ("    glLightModelf (GL_LIGHT_MODEL_TWO_SIDE, 0.0f);");
			 if (setCullFace) System.out.println ("    glEnable(GL_CULL_FACE);");
			 twoSided = false;
		}
		
		for (int j=0; j<4; j++) {
			if (materialAttributes.get(materialIndex)[j]) {
				System.out.println ("    glMaterialfv ("+renderSide+", GL_"+attributes[j].toUpperCase()+"" +
						", "+materialNames.get(materialIndex)+"_"+attributes[j]+");");
			}
		}
	}
	

	static void initEndObjFile() {
		if (!initDrawingRoutine) {
			printOpenGLInitEnd();
		}
		
	}
	static void printOpenGLInitEnd() {
		//init
		System.out.println("void\ninitDraw"+methodName+"(float *color)\n{");
		System.out.println(openGLinit+"\n");
		
		for (int i=0; i<materialNames.size(); i++)
		{
			float factor = 1.0f;
			Boolean copyColor = false;
			if (materialNames.get(i).substring(0,Math.min("userDefined_DS".length(), materialNames.get(i).length())).equalsIgnoreCase("userDefined_DS"))
			{
				try {
					if (materialNames.get(i).charAt("userDefined_DS".length())=='_')
						factor = 0.01f*Float.parseFloat(materialNames.get(i).substring("userDefined_DS".length()+1, materialNames.get(i).length()));
				}
				catch (Exception ex) { factor = 1.0f; }
				copyColor = true;
			}
			else if (materialNames.get(i).substring(0,Math.min("userDefined".length(), materialNames.get(i).length())).equalsIgnoreCase("userDefined"))
			{
				try {
					if (materialNames.get(i).charAt("userDefined".length())=='_')
						factor = 0.01f*Float.parseFloat(materialNames.get(i).substring("userDefined".length()+1, materialNames.get(i).length()));
				}
				catch (Exception ex) { factor = 1.0f; }
				copyColor = true;
			}
			
			if (copyColor)
			{
				System.out.println ("    copyColor((float *)"+materialNames.get(i)+"_"+attributes[1]+", color, "+(ambientFactor*factor)+");");
				System.out.println ("    copyColor((float *)"+materialNames.get(i)+"_"+attributes[2]+", color, "+(diffuseFactor*factor)+");");
				System.out.println ("    copyColor((float *)"+materialNames.get(i)+"_"+attributes[3]+", color, "+(specularFactor*factor)+");");
			}
		}
		
		System.out.println("}\n");
		
		
		//fin
		System.out.println("void\nfinDraw"+methodName+"()\n{");
		System.out.println(openGLfin+"\n}\n");

		System.out.println();
	}

	
	//spline S(t) = y0 + A*t + B*t^2 + C*t^3; 
	static float coefficientB(int j, int v, int i, float[][][] data) { //B(t+1)
		j%=fileCounter;
		if (j<0) j+=fileCounter;
		
		if (j==0 || j==fileCounter-1) return 0; //natural cubic spline

		int jm1 = j-1; if (jm1<0) jm1+=fileCounter;
		int jm2 = j-2; if (jm2<0) jm2+=fileCounter;
		int jm3 = j-3; if (jm3<0) jm3+=fileCounter;

		if (j==1) {
			
			float c = 3*(data[fileCounter-1][v][i]-2*data[fileCounter-2][v][i]+data[fileCounter-3][v][i]);
			
			float d=1.0f/4;
			
			for (int k=2; k<fileCounter-1; k++) {
				
				c=3*(data[fileCounter-k][v][i]-2*data[fileCounter-k-1][v][i]+data[fileCounter-k-2][v][i])-d*c;
				
				d = 1.0f/(4.0f-d);
			}
			
			c*=d;
			return c;
		}

		return 3*(data[j][v][i]-2*data[jm1][v][i]+data[jm2][v][i])
			   -coefficientB(jm2, v, i, data)-4*coefficientB(jm1, v, i, data);
	}
	static float coefficientA(float B, float Bp1, int j, int v, int i, float[][][] data) { //A(t)
		return (data[(j+1)%fileCounter][v][i]-data[j][v][i])-(Bp1+2*B)/3;
	}
	static float coefficientC(float B, float Bp1) { //C(t)
		return (Bp1-B)/3;
	}
}
