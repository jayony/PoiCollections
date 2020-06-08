/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.hslf.record;

import com.android.compaty.util.Optional;
import com.android.compaty.util.function.Supplier;

import org.apache.poi.ddf.DefaultEscherRecordFactory;
import org.apache.poi.ddf.EscherBoolProperty;
import org.apache.poi.ddf.EscherContainerRecord;
import org.apache.poi.ddf.EscherDgRecord;
import org.apache.poi.ddf.EscherOptRecord;
import org.apache.poi.ddf.EscherPropertyTypes;
import org.apache.poi.ddf.EscherRGBProperty;
import org.apache.poi.ddf.EscherRecord;
import org.apache.poi.ddf.EscherRecordTypes;
import org.apache.poi.ddf.EscherSimpleProperty;
import org.apache.poi.ddf.EscherSpRecord;
import org.apache.poi.ddf.EscherSpgrRecord;
import org.apache.poi.ddf.EscherTextboxRecord;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.util.GenericRecordUtil;
import org.apache.poi.util.IOUtils;
import org.apache.poi.util.LittleEndian;
import org.apache.poi.util.POILogger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * These are actually wrappers onto Escher drawings. Make use of
 *  the DDF classes to do useful things with them.
 * For now, creates a tree of the Escher records, and then creates any
 *  PowerPoint (hslf) records found within the EscherTextboxRecord
 *  (msofbtClientTextbox) records.
 * Also provides easy access to the EscherTextboxRecords, so that their
 *  text may be extracted and used in Sheets
 */

// For now, pretending to be an atom. Might not always be, but that
//  would require a wrapping class
public final class PPDrawing extends RecordAtom implements Iterable<EscherRecord> {

	//arbitrarily selected; may need to increase
	private static final int MAX_RECORD_LENGTH = 10_485_760;


	private byte[] _header;
	private long _type;

	private final List<EscherRecord> childRecords = new ArrayList<>();
	private EscherTextboxWrapper[] textboxWrappers;

	//cached EscherDgRecord
	private EscherDgRecord dg;

	/**
	 * Get access to the underlying Escher Records
	 */
	@SuppressWarnings("WeakerAccess")
	public List<EscherRecord> getEscherRecords() { return childRecords; }

	@Override
	public Iterator<EscherRecord> iterator() {
		return childRecords.iterator();
	}

	/**
	 * Get access to the atoms inside Textboxes
	 */
	public EscherTextboxWrapper[] getTextboxWrappers() { return textboxWrappers; }


	/* ******************** record stuff follows ********************** */

    /**
     * Creates a new, empty, PPDrawing (typically for use with a new Slide
     *  or Notes)
     */
    public PPDrawing() {
        _header = new byte[8];
        LittleEndian.putUShort(_header, 0, 15);
        LittleEndian.putUShort(_header, 2, RecordTypes.PPDrawing.typeID);
        LittleEndian.putInt(_header, 4, 0);

        textboxWrappers = new EscherTextboxWrapper[]{};
        create();
    }

	/**
	 * Sets everything up, groks the escher etc
	 */
	PPDrawing(byte[] source, int start, int len) {
		// Get the header
		_header = new byte[8];
		System.arraycopy(source,start,_header,0,8);

		// Get the type
		_type = LittleEndian.getUShort(_header,2);

		// Get the contents for now
		final byte[] contents = IOUtils.safelyAllocate(len, MAX_RECORD_LENGTH);
		System.arraycopy(source,start,contents,0,len);

		// Build up a tree of Escher records contained within
		final DefaultEscherRecordFactory erf = new HSLFEscherRecordFactory();
		findEscherChildren(erf, contents, 8, len-8, childRecords);
		EscherContainerRecord dgContainer = getDgContainer();

		if (dgContainer != null) {
			List<EscherTextboxWrapper> list = new ArrayList<>();
			List<EscherContainerRecord> container = findEscherContainer(dgContainer, EscherRecordTypes.SPGR_CONTAINER);
			for (EscherContainerRecord records : container) {
				List<EscherContainerRecord> recordList = findEscherContainer(records, EscherRecordTypes.SP_CONTAINER);
				for (EscherContainerRecord record : recordList) {
					EscherTextboxWrapper wrapper = getTextboxHelper(record);
					if (wrapper != null) {
						list.add(wrapper);
					}
				}
			}
			textboxWrappers = list.toArray(new EscherTextboxWrapper[0]);
		} else {
			// Find and EscherTextboxRecord's, and wrap them up
			final List<EscherTextboxWrapper> textboxes = new ArrayList<>();
			findEscherTextboxRecord(childRecords, textboxes);
			this.textboxWrappers = textboxes.toArray(new EscherTextboxWrapper[0]);
		}
	}

	private static EscherTextboxWrapper getTextboxHelper(EscherContainerRecord spContainer) {
		Optional<EscherTextboxRecord> oTB = firstEscherRecord(spContainer, EscherRecordTypes.CLIENT_TEXTBOX);
		if (!oTB.isPresent()) {
			return null;
		}

		EscherTextboxWrapper tbw = new EscherTextboxWrapper(oTB.get());
		findInSpContainer(spContainer).ifPresent(tbw::setStyleTextProp9Atom);

		Optional<EscherSpRecord> oSP = firstEscherRecord(spContainer, EscherRecordTypes.SP);
		oSP.map(EscherSpRecord::getShapeId).ifPresent(tbw::setShapeId);

		return tbw;
	}

	private static Optional<StyleTextProp9Atom> findInSpContainer(final EscherContainerRecord spContainer) {
		Optional<HSLFEscherClientDataRecord> oCD = firstEscherRecord(spContainer, EscherRecordTypes.CLIENT_DATA);
		if (!oCD.isPresent()) {
			return Optional.empty();
		}
		for (Record record : oCD.get().getHSLFChildRecords()) {
			if (sameHSLF(record, RecordTypes.ProgTags)) {
				Record[] childRecords = record.getChildRecords();
				for (Record childRecord : childRecords) {
					if (sameHSLF(childRecord, RecordTypes.ProgBinaryTag)) {
						StyleTextProp9Atom binaryTag = findInProgBinaryTag(childRecord);
						if (binaryTag != null) {
							return Optional.of(binaryTag);
						}
					}
				}
			}
		}
		return Optional.empty();
	}

	private static StyleTextProp9Atom findInProgBinaryTag(Record r) {
		Record[] ch = r.getChildRecords();
		if (ch != null &&
			ch.length == 2 &&
			ch[0] instanceof CString &&
			ch[1] instanceof BinaryTagDataBlob &&
			"___PPT9".equals(((CString)ch[0]).getText())
		) {
			BinaryTagDataBlob blob = (BinaryTagDataBlob) ch[1];
			StyleTextProp9Atom prop9 = (StyleTextProp9Atom) blob.findFirstOfType(RecordTypes.StyleTextProp9Atom.typeID);
			if (prop9 != null) {
				return prop9;
			}
		}
		return null;
	}

	/**
	 * Tree walking way of finding Escher Child Records
	 */
	private void findEscherChildren(DefaultEscherRecordFactory erf, byte[] source, int startPos, int lenToGo, List<EscherRecord> found) {

		int escherBytes = LittleEndian.getInt( source, startPos + 4 ) + 8;

		// Find the record
		EscherRecord r = erf.createRecord(source,startPos);
		// Fill it in
		r.fillFields( source, startPos, erf );
		// Save it
		found.add(r);

		// Wind on
		int size = r.getRecordSize();
		if(size < 8) {
			logger.log(POILogger.WARN, "Hit short DDF record at " + startPos + " - " + size);
		}

		/*
		 * Sanity check. Always advance the cursor by the correct value.
		 *
		 * getRecordSize() must return exactly the same number of bytes that was written in fillFields.
		 * Sometimes it is not so, see an example in bug #44770. Most likely reason is that one of ddf records calculates wrong size.
		 */
		if(size != escherBytes){
			logger.log(POILogger.WARN, "Record length=" + escherBytes + " but getRecordSize() returned " + r.getRecordSize() + "; record: " + r.getClass());
			size = escherBytes;
		}
		startPos += size;
		lenToGo -= size;
		if(lenToGo >= 8) {
			findEscherChildren(erf, source, startPos, lenToGo, found);
		}
	}

	/**
	 * Look for EscherTextboxRecords
	 */
	private void findEscherTextboxRecord(List<EscherRecord> toSearch, List<EscherTextboxWrapper> found) {
	    EscherSpRecord sp = null;
	    for (EscherRecord r : toSearch) {
	        if (r instanceof EscherSpRecord) {
	            sp = (EscherSpRecord)r;
	        } else if (r instanceof EscherTextboxRecord) {
				EscherTextboxRecord tbr = (EscherTextboxRecord)r;
				EscherTextboxWrapper w = new EscherTextboxWrapper(tbr);
				if (sp != null) {
				    w.setShapeId(sp.getShapeId());
				}
				found.add(w);
			} else if (r.isContainerRecord()) {
				// If it has children, walk them
				List<EscherRecord> children = r.getChildRecords();
				findEscherTextboxRecord(children,found);
			}
		}
	}

	/**
	 * We are type 1036
	 */
	public long getRecordType() { return _type; }

	/**
	 * We're pretending to be an atom, so return null
	 */
	public Record[] getChildRecords() { return null; }

	/**
	 * Write the contents of the record back, so it can be written
	 *  to disk
	 * Walks the escher layer to get the contents
	 */
	public void writeOut(OutputStream out) throws IOException {
		// Ensure the escher layer reflects the text changes
		for (EscherTextboxWrapper w : textboxWrappers) {
			w.writeOut(null);
		}

		// Find the new size of the escher children;
		int newSize = 0;
		for(EscherRecord er : childRecords) {
			newSize += er.getRecordSize();
		}

		// Update the size (header bytes 5-8)
		LittleEndian.putInt(_header,4,newSize);

		// Write out our header
		out.write(_header);

		// Now grab the children's data
		byte[] b = new byte[newSize];
		int done = 0;
		for(EscherRecord r : childRecords) {
		    done += r.serialize( done, b );
		}

		// Finally, write out the children
		out.write(b);
	}

	/**
	 * Create the Escher records associated with a new PPDrawing
	 */
	private void create(){
		EscherContainerRecord dgContainer = new EscherContainerRecord();
		dgContainer.setRecordId( EscherContainerRecord.DG_CONTAINER );
		dgContainer.setOptions((short)15);

		dg = new EscherDgRecord();
		dg.setOptions((short)16);
		dg.setNumShapes(1);
		dgContainer.addChildRecord(dg);

		EscherContainerRecord spgrContainer = new EscherContainerRecord();
		spgrContainer.setOptions((short)15);
		spgrContainer.setRecordId(EscherContainerRecord.SPGR_CONTAINER);

		EscherContainerRecord spContainer = new EscherContainerRecord();
		spContainer.setOptions((short)15);
		spContainer.setRecordId(EscherContainerRecord.SP_CONTAINER);

		EscherSpgrRecord spgr = new EscherSpgrRecord();
		spgr.setOptions((short)1);
		spContainer.addChildRecord(spgr);

		EscherSpRecord sp = new EscherSpRecord();
		sp.setOptions((short)((ShapeType.NOT_PRIMITIVE.nativeId << 4) + 2));
		sp.setFlags(EscherSpRecord.FLAG_PATRIARCH | EscherSpRecord.FLAG_GROUP);
		spContainer.addChildRecord(sp);
		spgrContainer.addChildRecord(spContainer);
		dgContainer.addChildRecord(spgrContainer);

		spContainer = new EscherContainerRecord();
		spContainer.setOptions((short)15);
		spContainer.setRecordId(EscherContainerRecord.SP_CONTAINER);
		sp = new EscherSpRecord();
		sp.setOptions((short)((ShapeType.RECT.nativeId << 4) + 2));
		sp.setFlags(EscherSpRecord.FLAG_BACKGROUND | EscherSpRecord.FLAG_HASSHAPETYPE);
		spContainer.addChildRecord(sp);

		EscherOptRecord opt = new EscherOptRecord();
		opt.setRecordId(EscherOptRecord.RECORD_ID);
		opt.addEscherProperty(new EscherRGBProperty(EscherPropertyTypes.FILL__FILLCOLOR, 134217728));
		opt.addEscherProperty(new EscherRGBProperty(EscherPropertyTypes.FILL__FILLBACKCOLOR, 134217733));
		opt.addEscherProperty(new EscherSimpleProperty(EscherPropertyTypes.FILL__RECTRIGHT, 10064750));
		opt.addEscherProperty(new EscherSimpleProperty(EscherPropertyTypes.FILL__RECTBOTTOM, 7778750));
		opt.addEscherProperty(new EscherBoolProperty(EscherPropertyTypes.FILL__NOFILLHITTEST, 1179666));
		opt.addEscherProperty(new EscherBoolProperty(EscherPropertyTypes.LINESTYLE__NOLINEDRAWDASH, 524288));
		opt.addEscherProperty(new EscherSimpleProperty(EscherPropertyTypes.SHAPE__BLACKANDWHITESETTINGS, 9));
		opt.addEscherProperty(new EscherSimpleProperty(EscherPropertyTypes.SHAPE__BACKGROUNDSHAPE, 65537));
		spContainer.addChildRecord(opt);

		dgContainer.addChildRecord(spContainer);
		
		childRecords.add(dgContainer);
	}

	/**
	 * Add a new EscherTextboxWrapper to this <code>PPDrawing</code>.
	 */
	public void addTextboxWrapper(EscherTextboxWrapper txtbox){
		EscherTextboxWrapper[] tw = new EscherTextboxWrapper[textboxWrappers.length + 1];
		System.arraycopy(textboxWrappers, 0, tw, 0, textboxWrappers.length);

		tw[textboxWrappers.length] = txtbox;
		textboxWrappers = tw;
	}

	/**
	 * @return the container record for drawings
	 * @since POI 3.14-Beta2
	 */
	public EscherContainerRecord getDgContainer() {
		return (EscherContainerRecord)firstEscherRecord(this, EscherRecordTypes.DG_CONTAINER).orElse(null);
	}
	
	/**
	 * Return EscherDgRecord which keeps track of the number of shapes and shapeId in this drawing group
	 *
	 * @return EscherDgRecord
	 */
	public EscherDgRecord getEscherDgRecord(){
		if (dg == null) {
			firstEscherRecord(this, EscherRecordTypes.DG_CONTAINER).
			flatMap(c -> firstEscherRecord((EscherContainerRecord)c, EscherRecordTypes.DG)).
			ifPresent(c -> dg = (EscherDgRecord)c);
		}
		return dg;
	}

    public StyleTextProp9Atom[] getNumberedListInfo() {
		EscherContainerRecord dgContainer = getDgContainer();
		StyleTextProp9Atom[] atoms;
		if (dgContainer == null) {
			atoms = new StyleTextProp9Atom[0];
		} else {
			List<StyleTextProp9Atom> list = new ArrayList<>();
			List<EscherContainerRecord> container = findEscherContainer(dgContainer, EscherRecordTypes.SPGR_CONTAINER);
			for (EscherContainerRecord records : container) {
				List<EscherContainerRecord> recordList = findEscherContainer(records, EscherRecordTypes.SP_CONTAINER);
				for (EscherContainerRecord record : recordList) {
					Optional<StyleTextProp9Atom> optional = findInSpContainer(record);
					if (optional.isPresent()) {
						list.add(optional.get());
					}
				}
			}
			atoms = (StyleTextProp9Atom[]) list.toArray();
		}
		return atoms;
    }

	@Override
	public Map<String, Supplier<?>> getGenericProperties() {
		return GenericRecordUtil.getGenericProperties("escherRecords", this::getEscherRecords);
	}

	private static boolean sameHSLF(Record record, RecordTypes type) {
		return record.getRecordType() == type.typeID;
	}

	private static boolean sameEscher(EscherRecord record, EscherRecordTypes type) {
		return record.getRecordId() == type.typeID;
	}

	@SuppressWarnings("unchecked")
	private static <T extends EscherRecord> Optional<T> firstEscherRecord(Iterable<EscherRecord> container, EscherRecordTypes type) {
		Iterator<EscherRecord> iterator = container.iterator();
		while (iterator.hasNext()) {
			EscherRecord next = iterator.next();
			if (sameEscher(next, type)) {
				return (Optional<T>) Optional.of(next);
			}
		}
		return Optional.empty();
	}

	private static List<EscherContainerRecord> findEscherContainer(EscherContainerRecord dgContainer, EscherRecordTypes type) {
		List<EscherContainerRecord> containers = dgContainer.getChildContainers();
		List<EscherContainerRecord> list = new ArrayList<>();
		for (EscherContainerRecord container : containers) {
			if (sameEscher(container, type)) {
				list.add(container);
			}
		}
		return list;
	}
}
