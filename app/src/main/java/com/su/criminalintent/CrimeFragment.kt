package com.su.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.util.*

private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "dialogDate"
private const val TAG = "CrimeFragment"
private const val REQUEST_CODE = 0
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2
private const val DATE_FORMAT = "yyyy년 M월 d일 H시 m분, E요일"

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks {
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this)[CrimeDetailViewModel::class.java]
    }

    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val arg = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = arg
            }
        }
    }

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable(ARG_CRIME_ID, UUID::class.java)
        } else {
            arguments?.getSerializable(ARG_CRIME_ID) as UUID
        }
        if (crimeId != null)
            crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        titleField = view.findViewById(R.id.crime_title)
        dateButton = view.findViewById(R.id.crime_date)
        solvedCheckBox = view.findViewById(R.id.crime_solved)
        reportButton = view.findViewById(R.id.crime_report)
        suspectButton = view.findViewById(R.id.crime_suspect)
        photoButton = view.findViewById(R.id.crime_camera)
        photoView = view.findViewById(R.id.crime_photo)

        dateButton.apply {
            text = crime.date.toString()
            //isEnabled = false
        }

        return view
    }

    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // 여기서는 이 함수의 실행 코드를 구현할 필요가 없어서 비워 둔다
            }

            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {
                // 여기서는 이 함수의 실행 코드를 구현할 필요가 없어서 비워 둔다
            }
        }

        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_CODE)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }

        reportButton.setOnClickListener {
            Log.d(TAG, "getCrimeReport: ${getCrimeReport()}")
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                //startActivity(intent)
                //항상 앱을 선택
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            intent.apply {
                startActivityForResult(this, REQUEST_CONTACT)
            }
        }

        photoButton.apply {
            val imgCaptureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val packageManager = requireActivity().packageManager
            setOnClickListener {
                imgCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                //카메라 앱 퍼미셔 부여
                val cameraActivities: List<ResolveInfo> = packageManager.queryIntentActivities(
                    imgCaptureIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )

                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

                startActivityForResult(imgCaptureIntent, REQUEST_PHOTO)

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT -> data?.data?.let { contactUri ->
                //쿼리에서 반환할 필드 지정
                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
                //쿼리 수행
                val cursor = requireActivity().contentResolver.query(
                    contactUri,
                    queryFields,
                    null,
                    null,
                    null
                )
                cursor?.let {
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }

            }

            requestCode == REQUEST_PHOTO -> {
                //퍼미션 회수
                requireActivity().revokeUriPermission(photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView()
            }
        }
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            photoView.setImageBitmap(bitmap)
        } else {
            photoView.setImageDrawable(null)
        }
    }

    //after run onCreateView called
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner, //fragment view가 유효할 때만 crimes 변경 감지
            Observer { crime ->
                crime?.let {
                    //Log.d(TAG, "Got crime ${crime}")
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "com.su.criminalintent.fileprovider",
                        photoFile
                    )
                    updateUI()
                }
            })
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState() //checkbox 깜빡임 제거
        }
        if (!crime.suspect.isNullOrBlank()) {
            suspectButton.text = crime.suspect
        }
        updatePhotoView()
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    private fun getCrimeReport(): String {
        val solved = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report, crime.title, dateString, solved, suspect)
    }
}